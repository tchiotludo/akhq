package org.kafkahq.repositories;

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
import org.kafkahq.models.Topic;
import org.kafkahq.modules.KafkaModule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class RecordRepository extends AbstractRepository implements Jooby.Module {
    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private TopicRepository topicRepository;

    public List<ConsumerRecord<String, String>> consume(String clusterId, List<String> topics, Options options) throws ExecutionException, InterruptedException {
        return  this.kafkaModule.debug(() -> {
            KafkaConsumer<String, String> consumer = this.kafkaModule.getConsumer(clusterId);
            options.seek(topicRepository, consumer, topics);

            List<ConsumerRecord<String, String>> list = new ArrayList<>();
            HashMap<Integer, Long> currentOffsets = new HashMap<>();
            boolean isDifferent = true;


            while (isDifferent) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                HashMap<Integer, Long> previousOffsets = new HashMap<>(currentOffsets);
                for (ConsumerRecord<String, String> record : records) {
                    list.add(record);
                    currentOffsets.put(record.partition(), record.offset());
                }

                isDifferent = !previousOffsets.equals(currentOffsets);
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

        private int size = 20;


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

        private Sort sort = Sort.OLDEST;

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

        private void seek(TopicRepository topicRepository, KafkaConsumer<String, String> consumer, List<String> topics) throws ExecutionException, InterruptedException {
            List<Topic> topicsDetails = topicRepository.findByName(topics);

            // list partitons
            List<TopicPartition> input = topicsDetails
                .stream()
                .flatMap(topic -> topic.getPartitions().stream().map(partition ->
                    new TopicPartition(topic.getName(), partition.getId())
                ))
                .collect(Collectors.toList());

            // filter partitions
            if (this.partition != null) {
                input = input.stream()
                    .filter(topicPartition -> topicPartition.partition() == this.partition)
                    .collect(Collectors.toList());
            }

            consumer.assign(input);

            // offset
            if (this.start == 0 && this.sort == Options.Sort.OLDEST) {
                consumer.seekToBeginning(input);
            } else {
                this.findOffset(topicsDetails)
                    .forEach(consumer::seek);
            }
        }

        private Map<TopicPartition, Long> findOffset(List<Topic> topicsDetails) {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(RecordRepository.class).toInstance(new RecordRepository());
    }
}
