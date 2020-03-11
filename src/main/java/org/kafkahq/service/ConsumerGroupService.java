package org.kafkahq.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.kafkahq.models.ConsumerGroup;
import org.kafkahq.modules.AbstractKafkaWrapper;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.repositories.ConsumerGroupRepository;
import org.kafkahq.repositories.RecordRepository;
import org.kafkahq.service.dto.ConsumerGroupd.ConsumerGroupDTO;
import org.kafkahq.service.dto.ConsumerGroupd.ConsumerGroupListDTO;
import org.kafkahq.service.mapper.ConsumerGroupMapper;
import org.kafkahq.utils.PagedList;
import org.kafkahq.utils.Pagination;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class ConsumerGroupService {
    private KafkaModule kafkaModule;
    private AbstractKafkaWrapper kafkaWrapper;
    private Environment environment;

    private ConsumerGroupRepository consumerGroupRepository;
    private RecordRepository recordRepository;

    private ConsumerGroupMapper consumerGroupMapper;

    @Value("${kafkahq.topic.default-view}")
    private String defaultView;
    @Value("${kafkahq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public ConsumerGroupService(KafkaModule kafkaModule, ConsumerGroupMapper consumerGroupMapper, AbstractKafkaWrapper kafkaWrapper,
                                ConsumerGroupRepository consumerGroupRepository, Environment environment, RecordRepository recordRepository) {
        this.kafkaModule = kafkaModule;
        this.consumerGroupMapper = consumerGroupMapper;
        this.kafkaWrapper = kafkaWrapper;
        this.consumerGroupRepository = consumerGroupRepository;
        this.environment = environment;
        this.recordRepository = recordRepository;
    }

    public ConsumerGroupListDTO getConsumerGroup(String clusterId, String view,Optional<String>  search, Optional<Integer> pageNumber)
            throws ExecutionException, InterruptedException {
        Pagination pagination = new Pagination(pageSize, pageNumber.orElse(1));

        PagedList<ConsumerGroup> list = this.consumerGroupRepository.list(clusterId, pagination, search);
        ArrayList<ConsumerGroupDTO>consumerGroupList=new ArrayList<>();
        list.stream().map(consumerGroup -> consumerGroupList.add(consumerGroupMapper.fromConsumerGroupToConsumerGroupDTO(consumerGroup))).collect(Collectors.toList());

        return new ConsumerGroupListDTO(consumerGroupList, list.total());
    }


}
