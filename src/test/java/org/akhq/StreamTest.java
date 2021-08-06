package org.akhq;

import com.yammer.metrics.core.Stoppable;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecord;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;

@Slf4j
public class StreamTest implements Runnable, Stoppable {
    private KafkaStreams streams;
    private String bootstrapServers;
    private String registryUrl;

    public StreamTest(String bootstrapServers, String registryUrl) {
        this.bootstrapServers = bootstrapServers;
        this.registryUrl = registryUrl;
    }

    @Override
    public void stop() {
        try {
            streams.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        StreamTest streamTest = new StreamTest("kafka:9092", "http://schema-registry:8085");
        streamTest.run();
    }

    @Override
    public void run() {
        streams = buildStream();
        start(streams);

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private void start(KafkaStreams streams) {
        streams.setUncaughtExceptionHandler(ex -> {
            log.error("Uncaught exception, closing Kafka Streams !", ex);
            System.exit(1);
            return null;
        });

        streams.setStateListener((newState, oldState) ->
            log.debug("Switching from {} to {} state", oldState, newState)
        );

        streams.start();
    }

    private KafkaStreams buildStream() {
        final Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "stream-test-example");
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "stream-test-example-client");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        streamsConfiguration.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.registryUrl);
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams/" + DigestUtils.md5Hex(this.bootstrapServers).toUpperCase());

        Serde<Cat> specificAvroSerde = getSpecificSerde();
        final StreamsBuilder builder = new StreamsBuilder();

        builder.stream(KafkaTestCluster.TOPIC_STREAM_IN, Consumed.with(Serdes.String(), Serdes.String()))
            .map((KeyValueMapper<String, String, KeyValue<String, Cat>>) (key, value) -> {
                String[] split = value.split(";");
                return new KeyValue<>(key, new Cat(Integer.valueOf(split[0]), split[1], Breed.valueOf(split[2])));
            })
            .through(KafkaTestCluster.TOPIC_STREAM_MAP, Produced.with(Serdes.String(), specificAvroSerde))
            .groupBy((key, value) -> String.valueOf(value.getId()), Grouped.with(Serdes.String(), specificAvroSerde))
            .count(Materialized.as("count"))
            .toStream()
            .map((key, value) -> {
                final GenericRecord count = new GenericData.Record(getSchema());
                count.put("key", key);
                count.put("count", value);
                return new KeyValue<>(key, count);
            })
            .to(KafkaTestCluster.TOPIC_STREAM_COUNT, Produced.with(Serdes.String(), getGenericRecordSerde()));

        return new KafkaStreams(builder.build(), streamsConfiguration);
    }

    private <T extends SpecificRecord> Serde<T> getSpecificSerde() {
        Serde<T> specificAvroSerde = new SpecificAvroSerde<>();
        specificAvroSerde.configure(
            Collections.singletonMap(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.registryUrl),
            false
        );
        return specificAvroSerde;
    }

    private Serde<GenericRecord> getGenericRecordSerde() {
        final Serde<GenericRecord> genericAvroSerde = new GenericAvroSerde();
        genericAvroSerde.configure(
            Collections.singletonMap(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.registryUrl),
            false
        );
        return genericAvroSerde;
    }

    private Schema getSchema() {
        final InputStream countSchema = StreamTest.class.getClassLoader().getResourceAsStream("avro/Count.avsc");
        try {
            return new Schema.Parser().parse(countSchema);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
