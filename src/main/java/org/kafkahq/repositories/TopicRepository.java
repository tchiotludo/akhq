package org.kafkahq.repositories;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.kafkahq.models.Partition;
import org.kafkahq.models.Topic;
import org.kafkahq.modules.KafkaModule;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Singleton
public class TopicRepository extends AbstractRepository {
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

    public List<Topic> list(Optional<String> search) throws ExecutionException, InterruptedException {
        ArrayList<String> list = new ArrayList<>();

        Collection<TopicListing> listTopics = kafkaWrapper.listTopics();

        for (TopicListing item : listTopics) {
            if (isSearchMatch(search, item.name())) {
                list.add(item.name());
            }
        }

        List<Topic> topics = this.findByName(list);
        topics.sort(Comparator.comparing(Topic::getName));

        return topics;
    }

    public Topic findByName(String name) throws ExecutionException, InterruptedException {
        Optional<Topic> topics = this.findByName(Collections.singletonList(name)).stream().findFirst();

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
}
