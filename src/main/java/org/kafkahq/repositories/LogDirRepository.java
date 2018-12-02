package org.kafkahq.repositories;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.requests.DescribeLogDirsResponse;
import org.jooby.Env;
import org.jooby.Jooby;
import org.kafkahq.models.LogDir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class LogDirRepository extends AbstractRepository implements Jooby.Module {
    public ArrayList<LogDir> list() throws ExecutionException, InterruptedException {
        ArrayList<LogDir> list = new ArrayList<>();

        for(Map.Entry<Integer, Map<String, DescribeLogDirsResponse.LogDirInfo>> nodes : kafkaWrapper.describeLogDir().entrySet()) {
            for(Map.Entry<String, DescribeLogDirsResponse.LogDirInfo> node: nodes.getValue().entrySet()) {
                for(Map.Entry<TopicPartition, DescribeLogDirsResponse.ReplicaInfo> log: node.getValue().replicaInfos.entrySet()) {
                    list.add(new LogDir(nodes.getKey(), node.getKey(), log.getKey(), log.getValue()));
                }
            }
        }

        return list;
    }

    public List<LogDir> findByTopic(String topic) throws ExecutionException, InterruptedException {
        return this.list().stream()
            .filter(item -> item.getTopic().equals(topic))
            .collect(Collectors.toList());
    }

    public List<LogDir> findByBroker(Integer brokerId) throws ExecutionException, InterruptedException {
        return this.list().stream()
            .filter(item -> item.getBrokerId().equals(brokerId))
            .collect(Collectors.toList());
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(LogDirRepository.class).toInstance(new LogDirRepository());
    }
}
