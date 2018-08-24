package org.kafkahq.repositories;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
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
    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private ConsumerGroupRepository consumerGroupRepository;

    @Inject
    private LogDirRepository logDirRepository;

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

    public void delete(String clusterId, String name) throws ExecutionException, InterruptedException {
        kafkaModule.getAdminClient(clusterId).deleteTopics(new ArrayList<String>() {{
            add(name);
        }}).all().get();
    }


    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(TopicRepository.class).toInstance(new TopicRepository());
    }
}
