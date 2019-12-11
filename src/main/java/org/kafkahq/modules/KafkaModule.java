package org.kafkahq.modules;

import com.google.common.collect.ImmutableMap;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.security.basicauth.BasicAuthCredentialProvider;
import io.confluent.kafka.schemaregistry.client.security.basicauth.BasicAuthCredentialProviderFactory;
import io.confluent.kafka.schemaregistry.client.security.basicauth.UserInfoCredentialProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.kafkahq.configs.AbstractProperties;
import org.kafkahq.configs.Connection;
import org.kafkahq.configs.Default;
import org.sourcelab.kafka.connect.apiclient.Configuration;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
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

    private Connection getConnection(String cluster) {
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

    private Map<String, KafkaProducer<String, String>> producers = new HashMap<>();

    public KafkaProducer<String, String> getProducer(String clusterId) {
        if (!this.producers.containsKey(clusterId)) {
            this.producers.put(clusterId, new KafkaProducer<>(
                this.getProducerProperties(clusterId),
                new StringSerializer(),
                new StringSerializer()
            ));
        }

        return this.producers.get(clusterId);
    }

    private Map<String, RestService> registryRestClient = new HashMap<>();

    public RestService getRegistryRestClient(String clusterId) {
        if (!this.registryRestClient.containsKey(clusterId)) {
            Connection connection = this.getConnection(clusterId);

            if (connection.getSchemaRegistry() != null) {
                RestService restService = new RestService(
                    connection.getSchemaRegistry().getUrl().toString()
                );

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

                this.registryRestClient.put(clusterId, restService);
            }
        }

        return this.registryRestClient.get(clusterId);
    }

    public SchemaRegistryClient getRegistryClient(String clusterId) {
        return new CachedSchemaRegistryClient(
            this.getRegistryRestClient(clusterId),
            Integer.MAX_VALUE
        );
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
                        configuration.useTrustStore(
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
