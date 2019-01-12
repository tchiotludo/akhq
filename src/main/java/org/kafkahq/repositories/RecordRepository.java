package org.kafkahq.repositories;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import lombok.*;
import lombok.experimental.Wither;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.jooby.Env;
import org.jooby.Jooby;
import org.kafkahq.models.Partition;
import org.kafkahq.models.Record;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.KafkaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class RecordRepository extends AbstractRepository implements Jooby.Module {
    private KafkaModule kafkaModule;

    private TopicRepository topicRepository;

    private static Logger logger = LoggerFactory.getLogger(RecordRepository.class);

    @Inject
    public RecordRepository(KafkaModule kafkaModule, TopicRepository topicRepository) {
        this.kafkaModule = kafkaModule;
        this.topicRepository = topicRepository;
    }

    public List<Record<String, String>> consume(Options options) throws ExecutionException, InterruptedException {
        return this.kafkaModule.debug(() -> {
            Topic topicsDetail = topicRepository.findByName(options.topic);


            if (options.sort == Options.Sort.OLDEST) {
                return consumeOldest(topicsDetail, options);
            } else {
                return consumeNewest(topicsDetail, options);
            }
        }, "Consume with options {}", options);
    }

    private List<Record<String, String>> consumeOldest(Topic topic, Options options) {
        KafkaConsumer<String, String> consumer = this.kafkaModule.getConsumer(options.clusterId);

        Map<TopicPartition, Long> partitions = topic
            .getPartitions()
            .stream()
            .map(partition -> getFirstOffsetForSortOldest(consumer, partition, options)
                .map(offsetBound -> offsetBound.withTopicPartition(
                    new TopicPartition(
                        partition.getTopic(),
                        partition.getId()
                    )
                ))
            )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(OffsetBound::getTopicPartition, OffsetBound::getBegin));

        List<Record<String, String>> list = new ArrayList<>();

        if (partitions.size() > 0) {
            consumer.assign(partitions.keySet());
            partitions.forEach(consumer::seek);

            partitions.forEach((topicPartition, first) ->
                logger.trace(
                    "Consume [topic: {}] [partition: {}] [start: {}]",
                    topicPartition.topic(),
                    topicPartition.partition(),
                    first
                )
            );

            synchronized (consumer) {
                ConsumerRecords<String, String> records = this.poll(consumer);

                for (ConsumerRecord<String, String> record : records) {
                    list.add(new Record<>(record));
                }
            }
        }
        return list;
    }

    private List<Record<String, String>> consumeNewest(Topic topic, Options options) {
        int pollSizePerPartition = pollSizePerPartition(topic, options);

        KafkaConsumer<String, String> consumer = this.kafkaModule.getConsumer(
            options.clusterId,
            pollSizePerPartition
        );

        return topic
            .getPartitions()
            .stream()
            .map(partition -> getOffsetForSortNewest(consumer, partition, options, pollSizePerPartition)
                .map(offset -> offset.withTopicPartition(
                    new TopicPartition(
                        partition.getTopic(),
                        partition.getId()
                    )
                ))
            )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(topicPartitionOffset -> {
                consumer.assign(Collections.singleton(topicPartitionOffset.getTopicPartition()));
                consumer.seek(topicPartitionOffset.getTopicPartition(), topicPartitionOffset.getBegin());
                List<Record<String, String>> list = new ArrayList<>();

                synchronized (consumer) {
                    int poll = 0;

                    do {
                        ConsumerRecords<String, String> records = this.poll(consumer);

                        if (records.isEmpty()) {
                            poll++;
                        } else {
                            poll = 0;
                        }

                        for (ConsumerRecord<String, String> record : records) {
                            if (record.offset() > topicPartitionOffset.getEnd()) {
                                poll = 2;
                                break;
                            }

                            list.add(new Record<>(record));
                        }
                    }
                    while (poll < 1);
                }

                Collections.reverse(list);

                return Stream.of(list);
            })
             .flatMap(List::stream)
             .collect(Collectors.toList());
    }

    private int pollSizePerPartition(Topic topic, Options options) {
        if (options.partition != null) {
            return options.size;
        } else {
            return (int) Math.round(options.size * 1.0 / topic.getPartitions().size());
        }
    }

    private Optional<Long> getFirstOffset(KafkaConsumer<String, String> consumer, Partition partition, Options options) {
        if (options.partition != null && partition.getId() != options.partition) {
            return Optional.empty();
        }

        long first = partition.getFirstOffset();

        if (options.timestamp != null) {
            Map<TopicPartition, OffsetAndTimestamp> timestampOffset = consumer.offsetsForTimes(
                ImmutableMap.of(
                    new TopicPartition(partition.getTopic(), partition.getId()),
                    options.timestamp
                )
            );

            for (Map.Entry<TopicPartition, OffsetAndTimestamp> entry : timestampOffset.entrySet()) {
                if (entry.getValue() == null) {
                    return Optional.empty();
                }

                first = entry.getValue().offset();
            }
        }

        return Optional.of(first);
    }

    private Optional<OffsetBound> getFirstOffsetForSortOldest(KafkaConsumer<String, String> consumer, Partition partition, Options options) {
        return getFirstOffset(consumer, partition, options)
            .map(first -> {
                if (options.after.size() > 0 && options.after.containsKey(partition.getId())) {
                    first = options.after.get(partition.getId()) + 1;
                }

                if (first > partition.getLastOffset()) {
                    return null;
                }

                return OffsetBound.builder()
                    .begin(first)
                    .build();
            });
    }

    private Optional<EndOffsetBound> getOffsetForSortNewest(KafkaConsumer<String, String> consumer, Partition partition, Options options, int pollSizePerPartition) {
        return getFirstOffset(consumer, partition, options)
            .map(first -> {
                long last = partition.getLastOffset();

                if (pollSizePerPartition > 0 && options.after.containsKey(partition.getId())) {
                    last = options.after.get(partition.getId()) - 1;
                }

                if (last == partition.getFirstOffset() || last < 0) {
                    return null;
                } else if (!(last - pollSizePerPartition < first)) {
                    first = last - pollSizePerPartition;
                }

                return EndOffsetBound.builder()
                    .begin(first)
                    .end(last)
                    .build();
            });
    }

    @SuppressWarnings("deprecation")
    private ConsumerRecords<String, String> poll(KafkaConsumer<String, String> consumer) {
        /*
        // poll with long call poll(final long timeoutMs, boolean includeMetadataInTimeout = true)
        // poll with Duration call poll(final long timeoutMs, boolean includeMetadataInTimeout = false)
        // So second one don't wait for metadata and return empty records
        // First one wait for metadata and send records
        // Hack bellow can be used to wait for metadata
        */
        return consumer.poll(1000);

        /*
        if (!records.isEmpty()) {
            return records;
        }

        Field field = consumer.getClass().getDeclaredField("client");
        field.setAccessible(true);

        ConsumerNetworkClient client = (ConsumerNetworkClient) field.get(consumer);

        while(!client.hasReadyNodes(System.currentTimeMillis())) {
            Thread.sleep(100);
        }

        return consumer.poll(Duration.ofMillis(2000));
        */
    }

    public void delete(String clusterId, String topic, Integer partition, String key) throws ExecutionException, InterruptedException {
        kafkaModule.getProducer(clusterId).send(new ProducerRecord<>(
            topic,
            partition,
            key,
            null
        )).get();
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class Options {
        public enum Sort {
            OLDEST,
            NEWEST,
        }

        private String clusterId;

        private String topic;

        public Options(String clusterId, String topic) {
            this.clusterId = clusterId;
            this.topic = topic;
        }

        private int size = 50;

        public void setSize(int size) {
            this.size = size;
        }

        private Map<Integer, Long> after = new HashMap<>();

        public void setAfter(String after) {
            this.after.clear();

            //noinspection UnstableApiUsage
            Splitter.on('_')
                .withKeyValueSeparator('-')
                .split(after)
                .forEach((key, value) -> this.after.put(new Integer(key), new Long(value)));
        }

        private Sort sort = Sort.OLDEST;

        public Sort getSort() {
            return sort;
        }

        public void setSort(Sort sort) {
            this.sort = sort;
        }

        private Integer partition;

        public Integer getPartition() {
            return partition;
        }

        public void setPartition(int partition) {
            this.partition = partition;
        }

        private Long timestamp;

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public URIBuilder after(List<Record<String, String>> records, URIBuilder uri) {
            Map<Integer, Long> next = new HashMap<>(this.after);
            for (Record<String, String> record : records) {
                if (this.sort == Sort.OLDEST && (!next.containsKey(record.getPartition()) || next.get(record.getPartition()) < record.getOffset())) {
                    next.put(record.getPartition(), record.getOffset());
                } else if (this.sort == Sort.NEWEST && (!next.containsKey(record.getPartition()) || next.get(record.getPartition()) > record.getOffset())) {
                    next.put(record.getPartition(), record.getOffset());
                }
            }

            if (records.size() == 0) {
                return URIBuilder.empty();
            }

            return offsetUrl(uri, next);
        }

        public URIBuilder before(List<Record<String, String>> records, URIBuilder uri) {
            /*
            Map<Integer, Long> previous = new HashMap<>(this.after);
            for (ConsumerRecord<String, String> record : records) {
                if (this.sort == Sort.OLDEST && (!previous.containsKey(record.partition()) || previous.get(record.partition()) > record.offset())) {
                    previous.put(record.partition(), record.offset());
                } else if (this.sort == Sort.NEWEST && (!previous.containsKey(record.partition()) || previous.get(record.partition()) < record.offset())) {
                    previous.put(record.partition(), record.offset());
                }
            }

            for (Integer key : previous.keySet()) {
                long offset = previous.get(key) - this.size;
                if (offset < 0) {
                    previous.remove(key);
                } else {
                    previous.put(key, offset);
                }
            }
            */

            return offsetUrl(uri, new HashMap<>());

            // return offsetUrl(basePath, previous);
        }

        private URIBuilder offsetUrl(URIBuilder uri, Map<Integer, Long> offsets) {
            ArrayList<String> segment = new ArrayList<>();

            for (Map.Entry<Integer, Long> offset : offsets.entrySet()) {
                segment.add(offset.getKey() + "-" + offset.getValue());
            }

            if (offsets.size() > 0) {
                uri = uri.addParameter("after", String.join("_", segment));
            }

            return uri;
        }
    }

    @Data
    @Builder
    private static class OffsetBound {
        @Wither
        private final TopicPartition topicPartition;
        private final long begin;
    }

    @Data
    @Builder
    private static class EndOffsetBound {
        @Wither
        private final TopicPartition topicPartition;
        private final long begin;
        private final long end;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(RecordRepository.class);
    }
}
