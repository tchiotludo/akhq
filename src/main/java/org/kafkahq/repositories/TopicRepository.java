package org.kafkahq.repositories;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.jooby.Env;
import org.jooby.Jooby;
import org.kafkahq.models.Partition;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.KafkaModule;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Singleton
public class TopicRepository extends AbstractRepository implements Jooby.Module {
    private KafkaModule kafkaModule;

    private ConsumerGroupRepository consumerGroupRepository;

    private LogDirRepository logDirRepository;

    private ConfigRepository configRepository;

    @Inject
    public TopicRepository(KafkaModule kafkaModule, ConsumerGroupRepository consumerGroupRepository, LogDirRepository logDirRepository, ConfigRepository configRepository) {
        this.kafkaModule = kafkaModule;
        this.consumerGroupRepository = consumerGroupRepository;
        this.logDirRepository = logDirRepository;
        this.configRepository = configRepository;
    }

    public List<Topic> list() throws ExecutionException, InterruptedException {
        ArrayList<String> list = new ArrayList<>();

        Collection<TopicListing> listTopics = kafkaWrapper.listTopics();

        for (TopicListing item : listTopics) {
            list.add(item.name());
        }

        List<Topic> topics = this.findByName(list);
        topics.sort(Comparator.comparing(Topic::getName));

        return topics;
    }

    public Topic findByName(String name) throws ExecutionException, InterruptedException {
        Optional<Topic> topics = this.findByName(new ArrayList<String>() {{
                add(name);
            }}).stream().findFirst();

        return topics.orElseThrow(() -> new NoSuchElementException("Topic '" + name + "' doesn't exist"));
    }

    public List<Topic> findByName(List<String> topics) throws ExecutionException, InterruptedException {
        ArrayList<Topic> list = new ArrayList<>();

        Set<Map.Entry<String, TopicDescription>> topicDescriptions = kafkaWrapper.describeTopics(topics).entrySet();
        Map<String, List<Partition.Offsets>> topicOffsets = kafkaWrapper.describeTopicsOffsets(topics);

        for (Map.Entry<String, TopicDescription> description : topicDescriptions) {
            list.add(
                new Topic(
                    description.getValue(),
                    consumerGroupRepository.findByTopic(description.getValue().name()),
                    logDirRepository.findByTopic(description.getValue().name()),
                    topicOffsets.get(description.getValue().name())
                )
            );
        }

        return list;
    }

    public void create(String clusterId, String name, int partitions, short replicationFactor, List<org.kafkahq.models.Config> configs) throws ExecutionException, InterruptedException {
        kafkaModule
            .getAdminClient(clusterId)
            .createTopics(Collections.singleton(new NewTopic(name, partitions, replicationFactor)))
            .all()
            .get();

        configRepository.updateTopic(clusterId, name, configs);
    }

    public void delete(String clusterId, String name) throws ExecutionException, InterruptedException {
        kafkaModule.getAdminClient(clusterId)
            .deleteTopics(Collections.singleton(name))
            .all()
            .get();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(TopicRepository.class).asEagerSingleton();
    }
}
