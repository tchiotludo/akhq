package org.akhq.modules;

import com.google.common.collect.ImmutableMap;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.security.SslFactory;
import io.confluent.kafka.schemaregistry.client.security.basicauth.BasicAuthCredentialProvider;
import io.confluent.kafka.schemaregistry.client.security.basicauth.BasicAuthCredentialProviderFactory;
import io.confluent.kafka.schemaregistry.client.security.basicauth.UserInfoCredentialProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.akhq.configs.AbstractProperties;
import org.akhq.configs.Connection;
import org.akhq.configs.Default;
import org.sourcelab.kafka.connect.apiclient.Configuration;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class KafkaModule {
    @Inject
    private List<Connection> connections;

    @Inject
    private List<Default> defaults;

    public List<String> getClustersList() {
        return this.connections
            .stream()
            .map(r -> r.getName().split("\\.")[0])
            .distinct()
            .collect(Collectors.toList());
    }

    public Connection getConnection(String cluster) {
        return this.connections
            .stream()
            .filter(r -> r.getName().equals(cluster))
            .findFirst()
            .get();
    }

    private Properties getDefaultsProperties(List<? extends AbstractProperties> current, String type) {
        Properties properties = new Properties();

        current
            .stream()
            .filter(r -> r.getName().equals(type))
            .forEach(r -> r.getProperties()
                .forEach(properties::put)
            );

        return properties;
    }

    private Properties getConsumerProperties(String clusterId) {
        Properties props = new Properties();
        props.putAll(this.getDefaultsProperties(this.defaults, "consumer"));
        props.putAll(this.getDefaultsProperties(this.connections, clusterId));

        return props;
    }

    private Properties getProducerProperties(String clusterId) {
        Properties props = new Properties();
        props.putAll(this.getDefaultsProperties(this.defaults, "producer"));
        props.putAll(this.getDefaultsProperties(this.connections, clusterId));

        return props;
    }

    private Properties getAdminProperties(String clusterId) {
        Properties props = new Properties();
        props.putAll(this.getDefaultsProperties(this.defaults, "admin"));
        props.putAll(this.getDefaultsProperties(this.connections, clusterId));

        return props;
    }

    private Map<String, AdminClient> adminClient = new HashMap<>();

    public AdminClient getAdminClient(String clusterId) {
        if (!this.adminClient.containsKey(clusterId)) {
            this.adminClient.put(clusterId, AdminClient.create(this.getAdminProperties(clusterId)));
        }

        return this.adminClient.get(clusterId);
    }

    public KafkaConsumer<byte[], byte[]> getConsumer(String clusterId) {
        return new KafkaConsumer<>(
            this.getConsumerProperties(clusterId),
            new ByteArrayDeserializer(),
            new ByteArrayDeserializer()
        );
    }

    public KafkaConsumer<byte[], byte[]> getConsumer(String clusterId, Properties properties) {
        Properties props = this.getConsumerProperties(clusterId);
        props.putAll(properties);

        return new KafkaConsumer<>(
            props,
            new ByteArrayDeserializer(),
            new ByteArrayDeserializer()
        );
    }

    private Map<String, KafkaProducer<byte[], byte[]>> producers = new HashMap<>();

    public KafkaProducer<byte[], byte[]> getProducer(String clusterId) {
        if (!this.producers.containsKey(clusterId)) {
            this.producers.put(clusterId, new KafkaProducer<>(
                this.getProducerProperties(clusterId),
                new ByteArraySerializer(),
                new ByteArraySerializer()
            ));
        }

        return this.producers.get(clusterId);
    }

    public AvroSchemaProvider getAvroSchemaProvider(String clusterId) {
        AvroSchemaProvider avroSchemaProvider = new AvroSchemaProvider();
        avroSchemaProvider.configure(Collections.singletonMap(
            "schemaVersionFetcher",
            new CachedSchemaRegistryClient(this.getRegistryRestClient(clusterId), 100)
        ));
        return avroSchemaProvider;
    }

    public RestService getRegistryRestClient(String clusterId) {
        Connection connection = this.getConnection(clusterId);

        if (connection.getSchemaRegistry() != null) {
            RestService restService = new RestService(
                connection.getSchemaRegistry().getUrl()
            );

            if (connection.getSchemaRegistry().getProperties() != null
                    && !connection.getSchemaRegistry().getProperties().isEmpty()) {

                Map<String, Object> sslConfigs =
                        (Map) connection.getSchemaRegistry().getProperties().entrySet().stream().filter((e) -> {
                    return ((String) e.getKey()).startsWith("schema.registry.");
                }).collect(Collectors.toMap((e) -> {
                    return ((String) e.getKey()).substring("schema.registry.".length());
                }, Map.Entry::getValue));

                SslFactory sslFactory = new SslFactory(sslConfigs);
                if (sslFactory != null && sslFactory.sslContext() != null) {
                    restService.setSslSocketFactory(sslFactory.sslContext().getSocketFactory());
                }
            }

            restService.setHttpHeaders(Collections.singletonMap("Accept", "application/json"));

            if (connection.getSchemaRegistry().getBasicAuthUsername() != null) {
                BasicAuthCredentialProvider basicAuthCredentialProvider = BasicAuthCredentialProviderFactory
                    .getBasicAuthCredentialProvider(
                        new UserInfoCredentialProvider().alias(),
                        ImmutableMap.of(
                            "schema.registry.basic.auth.user.info",
                            connection.getSchemaRegistry().getBasicAuthUsername() + ":" +
                                connection.getSchemaRegistry().getBasicAuthPassword()
                        )
                    );
                restService.setBasicAuthCredentialProvider(basicAuthCredentialProvider);
            }

            if (connection.getSchemaRegistry().getProperties() != null) {
                restService.configure(connection.getSchemaRegistry().getProperties());
            }
            return restService;
        }
        return null;
    }

    private Map<String, SchemaRegistryClient> registryClient = new HashMap<>();

    public SchemaRegistryClient getRegistryClient(String clusterId) {
        if (!this.registryClient.containsKey(clusterId)) {
            Connection connection = this.getConnection(clusterId);

            SchemaRegistryClient client = new CachedSchemaRegistryClient(
                this.getRegistryRestClient(clusterId),
                Integer.MAX_VALUE,
                connection.getSchemaRegistry() != null ? connection.getSchemaRegistry().getProperties() : null
            );

            this.registryClient.put(clusterId, client);
        }

        return this.registryClient.get(clusterId);
    }

    private Map<String, Map<String, KafkaConnectClient>> connectRestClient = new HashMap<>();

    public Map<String, KafkaConnectClient> getConnectRestClient(String clusterId) {
        if (!this.connectRestClient.containsKey(clusterId)) {
            Connection connection = this.getConnection(clusterId);

            if (connection.getConnect() != null && !connection.getConnect().isEmpty()) {

                Map<String,KafkaConnectClient> mapConnects = new HashMap<>();
                connection.getConnect().forEach(connect -> {

                    URIBuilder uri = URIBuilder.fromString(connect.getUrl().toString());
                    Configuration configuration = new Configuration(uri.toNormalizedURI(false).toString());

                    if (connect.getBasicAuthUsername() != null) {
                        configuration.useBasicAuth(
                                connect.getBasicAuthUsername(),
                                connect.getBasicAuthPassword()
                        );
                    }

                    if (connect.getSslTrustStore() != null) {
                        configuration.useTrustStore(
                                new File(connect.getSslTrustStore()),
                                connect.getSslTrustStorePassword()
                        );
                    }

                    if (connect.getSslKeyStore() != null) {
                        configuration.useKeyStore(
                                new File(connect.getSslKeyStore()),
                                connect.getSslKeyStorePassword()
                        );
                    }
                    mapConnects.put(connect.getName(), new KafkaConnectClient(configuration));
                });
                this.connectRestClient.put(clusterId, mapConnects);
            }
        }

        return this.connectRestClient.get(clusterId);
    }

}
