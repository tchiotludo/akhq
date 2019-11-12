package org.kafkahq.repositories;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.requests.DescribeLogDirsResponse;
import org.kafkahq.models.LogDir;
import org.kafkahq.modules.KafkaWrapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class LogDirRepository extends AbstractRepository {
    @Inject
    KafkaWrapper kafkaWrapper;

    public ArrayList<LogDir> list(String clusterId) throws ExecutionException, InterruptedException {
        ArrayList<LogDir> list = new ArrayList<>();

        for(Map.Entry<Integer, Map<String, DescribeLogDirsResponse.LogDirInfo>> nodes : kafkaWrapper.describeLogDir(clusterId).entrySet()) {
            for(Map.Entry<String, DescribeLogDirsResponse.LogDirInfo> node: nodes.getValue().entrySet()) {
                for(Map.Entry<TopicPartition, DescribeLogDirsResponse.ReplicaInfo> log: node.getValue().replicaInfos.entrySet()) {
                    list.add(new LogDir(nodes.getKey(), node.getKey(), log.getKey(), log.getValue()));
                }
            }
        }

        return list;
    }

    public List<LogDir> findByTopic(String clusterId, String topic) throws ExecutionException, InterruptedException {
        return this.list(clusterId).stream()
            .filter(item -> item.getTopic().equals(topic))
            .collect(Collectors.toList());
    }

    public List<LogDir> findByBroker(String clusterId, Integer brokerId) throws ExecutionException, InterruptedException {
        return this.list(clusterId).stream()
            .filter(item -> item.getBrokerId().equals(brokerId))
            .collect(Collectors.toList());
    }
}
