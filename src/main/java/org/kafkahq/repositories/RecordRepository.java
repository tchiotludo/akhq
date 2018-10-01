package org.kafkahq.repositories;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.jooby.Env;
import org.jooby.Jooby;
import org.kafkahq.models.Partition;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.KafkaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

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

    public List<ConsumerRecord<String, String>> consume(String clusterId, List<String> topics, Options options) throws ExecutionException, InterruptedException {
        return  this.kafkaModule.debug(() -> {
            List<Topic> topicsDetail = topicRepository.findByName(topics);
            KafkaConsumer<String, String> consumer = this.kafkaModule.getConsumer(clusterId);

            Map<String, Map<Integer, Long>> assigments = new TreeMap<>();
            List<ConsumerRecord<String, String>> list = new ArrayList<>();

            synchronized (consumer) {
                topicsDetail.forEach(topic -> topic
                    .getPartitions()
                    .forEach(partition -> {
                        assigments.putAll(options.seek(consumer, partition));

                        Long endOffset = assigments.get(partition.getTopic()).get(partition.getId());
                        long currentOffset = 0L;
                        int emptyResult = 0;

                        while (currentOffset < endOffset && emptyResult <= 1) {
                            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(2000));

                            if (records.isEmpty()) {
                                emptyResult++;
                            } else {
                                emptyResult = 0;
                            }

                            for (ConsumerRecord<String, String> record : records) {
                                logger.trace(
                                    "Record topic {} partion {} offset {}",
                                    partition.getTopic(),
                                    partition.getId(),
                                    record.offset()
                                );

                                currentOffset = record.offset();

                                if (currentOffset >= endOffset) {
                                    break;
                                } else {
                                    list.add(record);
                                }
                            }
                        }
                    })
                );
            }

            return list;
        }, "Consume {} with options {}", topics, options);
    }

    @ToString
    @EqualsAndHashCode
    public static class Options {
        public enum Sort {
            OLDEST,
            NEWEST,
        }

        private int size = 10;


        public int getSize() {
            return size;
        }

        public Options setSize(int size) {
            this.size = size;

            return this;
        }

        private int start = 0;

        public int getStart() {
            return start;
        }

        public Options setStart(int start) {
            this.start = start;

            return this;
        }

        private Sort sort = Sort.NEWEST;

        public Sort getSort() {
            return sort;
        }

        public Options setSort(Sort sort) {
            this.sort = sort;

            return this;
        }

        private Pattern filter;

        public Pattern getFilter() {
            return filter;
        }

        public void setFilter(Pattern filter) {
            this.filter = filter;
        }

        public Integer partition;

        public Integer getPartition() {
            return partition;
        }

        public Options setPartition(int partition) {
            this.partition = partition;

            return this;
        }

        public Map<String, Map<Integer, Long>> seek(KafkaConsumer<String, String> consumer, Partition partition) {
            TopicPartition topicPartition = new TopicPartition(
                partition.getTopic(),
                partition.getId()
            );

            consumer.assign(Collections.singleton(topicPartition));
            long begin;
            long last;

            if (this.start == 0 && this.sort == Options.Sort.OLDEST) {
                begin = partition.getFirstOffset();
                last = partition.getFirstOffset() + this.size > partition.getLastOffset() ?
                    partition.getLastOffset() :
                    partition.getFirstOffset() + this.size;
            } else {
                if (partition.getLastOffset() - this.size < partition.getFirstOffset()) {
                    begin = partition.getFirstOffset();
                    last = partition.getLastOffset();
                } else {
                    begin = partition.getLastOffset() - this.size;
                    last = partition.getLastOffset();
                }
            }

            consumer.seek(topicPartition, begin);

            logger.trace(
                "Consume topic {} partion {} from {} to {}",
                partition.getTopic(),
                partition.getId(),
                begin,
                last - 1
            );

            return ImmutableMap.of(partition.getTopic(), ImmutableMap.of(partition.getId(), last));
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(RecordRepository.class);
    }
}
