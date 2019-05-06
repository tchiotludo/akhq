package org.kafkahq.repositories;

import io.confluent.ksql.rest.client.RestResponse;
import io.confluent.ksql.rest.entity.KsqlEntityList;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.utils.Debug;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KsqlRepository extends AbstractRepository {
    private KafkaModule kafkaModule;

    @Inject
    public KsqlRepository(KafkaModule kafkaModule) {
        this.kafkaModule = kafkaModule;
    }

    public void showProperties(String clusterId) {
        RestResponse<KsqlEntityList> ksqlEntityListRestResponse = this.kafkaModule
            .getKsqlRestClient(clusterId)
            .makeKsqlRequest("SHOW FUNCTIONS;");

        Debug.print(ksqlEntityListRestResponse);
    }
}
