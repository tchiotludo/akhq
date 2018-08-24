package org.kafkahq.repositories;

import org.kafkahq.modules.KafkaWrapper;

abstract public class AbstractRepository {
    protected static KafkaWrapper kafkaWrapper;

    public static void setWrapper(KafkaWrapper kafkaWrapper) {
        AbstractRepository.kafkaWrapper = kafkaWrapper;
    }
}
