package org.kafkahq.repositories;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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

import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
            KafkaConsumer<String, String> consumer = this.kafkaModule.getConsumer(options.clusterId);

            List<Record<String, String>> list = new ArrayList<>();
            HashMap<TopicPartition, OffsetBound<Long, Long>> partitionsOffset = new HashMap<>();

            for (Partition partition : topicsDetail.getPartitions()) {
                OffsetBound<Long, Long> seek = options.seek(partition);

                if (seek == null) {
                    logger.trace(
                        "Skip Consume topic {} partion {} (first {}, last {})",
                        partition.getTopic(),
                        partition.getId(),
                        partition.getFirstOffset(),
                        partition.getLastOffset()
                    );
                } else {
                    logger.trace(
                        "Consume topic {} partion {} from {} to {} (first {}, last {})",
                        partition.getTopic(),
                        partition.getId(),
                        seek.getBegin(),
                        seek.getEnd(),
                        partition.getFirstOffset(),
                        partition.getLastOffset()
                    );


                    TopicPartition topicPartition = new TopicPartition(
                        partition.getTopic(),
                        partition.getId()
                    );

                    partitionsOffset.putAll(ImmutableMap.of(topicPartition, seek));
                }
            }

            Map<Integer, Long> lastOffset = partitionsOffset.entrySet().stream().collect(
                Collectors.toMap(o -> o.getKey().partition(), o -> o.getValue().getEnd())
            );

            synchronized (consumer) {
                consumer.assign(partitionsOffset.keySet());
                partitionsOffset.forEach((topicPartition, offsetBound) ->
                    consumer.seek(topicPartition, offsetBound.getBegin())
                );

                if (partitionsOffset.size() > 0) {
                    ConsumerRecords<String, String> records = this.poll(consumer);

                    for (ConsumerRecord<String, String> record : records) {
                        if (record.offset() > lastOffset.get(record.partition())) {
                            continue;
                        }

                        /*
                        logger.trace(
                            "Record topic {} partion {} offset {}",
                            record.topic(),
                            record.partition(),
                            record.offset()
                        );
                        */

                        list.add(new Record<>(record));
                    }
                }
            }

            if (options.sort == Options.Sort.NEWEST) {
                Collections.reverse(list);
            }

            return list;
        }, "Consume with options {}", options);
    }

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

        private int size = 100;

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

        private Sort sort = Sort.NEWEST;

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

        private OffsetBound<Long, Long> seek(Partition partition) {
            long begin;
            long first = partition.getFirstOffset();
            long last = partition.getLastOffset();

            if (this.partition != null && partition.getId() != this.partition) {
                return null;
            }

            switch (this.sort) {
                case OLDEST:
                    if (this.after.size() > 0 && this.after.containsKey(partition.getId())) {
                        first = this.after.get(partition.getId()) + 1;
                    }

                    begin = first;
                    last = first + this.size > partition.getLastOffset() ? partition.getLastOffset() : first + this.size;

                    if (begin > partition.getLastOffset()) {
                        return null;
                    }

                    return OffsetBound.of(begin, last);

                case NEWEST:
                    if (this.after.size() > 0 && this.after.containsKey(partition.getId())) {
                        last = this.after.get(partition.getId()) - 1;
                    }

                    if (last == partition.getFirstOffset() || last < 0) {
                        return null;
                    } else if (last - this.size < first) {
                        begin = first;
                    } else {
                        begin = last - this.size;
                    }

                    return OffsetBound.of(begin, last);
            }

            return null;
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

        public URIBuilder before(List<Record<String, String>> records, URIBuilder uri) throws URISyntaxException {
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

    @Data(staticConstructor = "of")
    static public class OffsetBound<A, B> {
        private final A begin;
        private final B end;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(RecordRepository.class);
    }
}
