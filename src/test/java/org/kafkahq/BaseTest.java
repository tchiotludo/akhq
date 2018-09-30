package org.kafkahq;

import com.salesforce.kafka.test.KafkaTestUtils;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.modules.KafkaWrapper;
import org.kafkahq.repositories.AbstractRepository;
import org.kafkahq.repositories.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseTest {
    private static Logger logger = LoggerFactory.getLogger(RecordRepository.class);

    public static App app = new App();

    @ClassRule
    public static final SharedKafkaTestResource kafkaTestResource = new SharedKafkaTestResource()
        .withBrokers(3);

    @Before
    public void setup() {
        // kafka test connections
        app.use(ConfigFactory
            .empty()
            .withValue(
                "kafka.connections.test.bootstrap.servers",
                ConfigValueFactory.fromAnyRef(kafkaTestResource.getKafkaConnectString())
            )
            .withFallback(ConfigFactory.load("application"))
        );

        // bootstrap app
        app.start("test");
        AbstractRepository.setWrapper(new KafkaWrapper(app.require(KafkaModule.class), "test"));

        // test data
        KafkaTestUtils kafkaTestUtils = kafkaTestResource.getKafkaTestUtils();

        kafkaTestUtils.createTopic("empty", 12, (short) 3);

        kafkaTestUtils.createTopic("random", 3, (short) 3);
        for (int i = 0; i < 3; i++) {
            kafkaTestUtils.produceRecords(100, "random", i);
        }
        logger.info("Kafka server started with test data on {}", kafkaTestResource.getKafkaConnectString());
    }

    @After
    public void tearDown() {
        app.stop();
    }
}