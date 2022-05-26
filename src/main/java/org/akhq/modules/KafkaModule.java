package org.akhq.modules;

import com.google.common.collect.ImmutableMap;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.RestService;
import io.confluent.kafka.schemaregistry.client.security.SslFactory;
import io.confluent.kafka.schemaregistry.client.security.basicauth.BasicAuthCredentialProvider;
import io.confluent.kafka.schemaregistry.client.security.basicauth.BasicAuthCredentialProviderFactory;
import io.confluent.kafka.schemaregistry.client.security.basicauth.UserInfoCredentialProvider;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.akhq.configs.AbstractProperties;
import org.akhq.configs.Connection;
import org.akhq.configs.Default;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.sourcelab.kafka.connect.apiclient.Configuration;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
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

    public boolean clusterExists(String cluster){
        return this.getClustersList().contains(cluster);
    }

    public Connection getConnection(String cluster) throws InvalidClusterException {
        if (!this.clusterExists(cluster)) {
            throw new InvalidClusterException("Invalid cluster '" + cluster + "'");
        }

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
            .forEach(r -> properties.putAll(r.getProperties()));

        return properties;
    }

    private Properties getConsumerProperties(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        Properties props = new Properties();
        props.putAll(this.getDefaultsProperties(this.defaults, "consumer"));
        props.putAll(this.getDefaultsProperties(this.connections, clusterId));

        return props;
    }

    private Properties getProducerProperties(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        Properties props = new Properties();
        props.putAll(this.getDefaultsProperties(this.defaults, "producer"));
        props.putAll(this.getDefaultsProperties(this.connections, clusterId));

        return props;
    }

    private Properties getAdminProperties(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        Properties props = new Properties();
        props.putAll(this.getDefaultsProperties(this.defaults, "admin"));
        props.putAll(this.getDefaultsProperties(this.connections, clusterId));

        return props;
    }

    private final Map<String, AdminClient> adminClient = new HashMap<>();

    public AdminClient getAdminClient(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        if (!this.adminClient.containsKey(clusterId)) {
            this.adminClient.put(clusterId, AdminClient.create(this.getAdminProperties(clusterId)));
        }

        return this.adminClient.get(clusterId);
    }

    public KafkaConsumer<byte[], byte[]> getConsumer(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        return new KafkaConsumer<>(
            this.getConsumerProperties(clusterId),
            new ByteArrayDeserializer(),
            new ByteArrayDeserializer()
        );
    }

    public KafkaConsumer<byte[], byte[]> getConsumer(String clusterId, Properties properties) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        Properties props = this.getConsumerProperties(clusterId);
        props.putAll(properties);

        return new KafkaConsumer<>(
            props,
            new ByteArrayDeserializer(),
            new ByteArrayDeserializer()
        );
    }

    private final Map<String, KafkaProducer<byte[], byte[]>> producers = new HashMap<>();

    public KafkaProducer<byte[], byte[]> getProducer(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        if (!this.producers.containsKey(clusterId)) {
            this.producers.put(clusterId, new KafkaProducer<>(
                this.getProducerProperties(clusterId),
                new ByteArraySerializer(),
                new ByteArraySerializer()
            ));
        }

        return this.producers.get(clusterId);
    }

    public AvroSchemaProvider getAvroSchemaProvider(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        AvroSchemaProvider avroSchemaProvider = new AvroSchemaProvider();
        avroSchemaProvider.configure(Collections.singletonMap(
            "schemaVersionFetcher",
            new CachedSchemaRegistryClient(this.getRegistryRestClient(clusterId), 1000)
        ));
        return avroSchemaProvider;
    }

    public JsonSchemaProvider getJsonSchemaProvider(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        JsonSchemaProvider jsonSchemaProvider = new JsonSchemaProvider();
        jsonSchemaProvider.configure(Collections.singletonMap(
            "schemaVersionFetcher",
            new CachedSchemaRegistryClient(this.getRegistryRestClient(clusterId), 1000)
        ));

        return jsonSchemaProvider;
    }

    public ProtobufSchemaProvider getProtobufSchemaProvider(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        ProtobufSchemaProvider protobufSchemaProvider = new ProtobufSchemaProvider();
        protobufSchemaProvider.configure(Collections.singletonMap(
            "schemaVersionFetcher",
            new CachedSchemaRegistryClient(this.getRegistryRestClient(clusterId), 1000)
        ));

        return protobufSchemaProvider;
    }

    public RestService getRegistryRestClient(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        Connection connection = this.getConnection(clusterId);

        if (connection.getSchemaRegistry() != null) {
            RestService restService = new RestService(
                connection.getSchemaRegistry().getUrl()
            );

            if (connection.getSchemaRegistry().getProperties() != null
                && !connection.getSchemaRegistry().getProperties().isEmpty()) {

                Map<String, Object> sslConfigs =
                    connection
                        .getSchemaRegistry()
                        .getProperties()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getKey().startsWith("schema.registry."))
                        .collect(Collectors.toMap(e -> e.getKey().substring("schema.registry.".length()), Map.Entry::getValue));

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

    private final Map<String, SchemaRegistryClient> registryClient = new HashMap<>();


    public SchemaRegistryClient getRegistryClient(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        if (!this.registryClient.containsKey(clusterId)) {
            Connection connection = this.getConnection(clusterId);


            List<SchemaProvider> providers = new ArrayList<>();
            providers.add(new AvroSchemaProvider());
            providers.add(new JsonSchemaProvider());
            providers.add(new ProtobufSchemaProvider());

            SchemaRegistryClient client = new CachedSchemaRegistryClient(
                this.getRegistryRestClient(clusterId),
                1000,
                providers,
                connection.getSchemaRegistry() != null ? connection.getSchemaRegistry().getProperties() : null,
                null
            );

            this.registryClient.put(clusterId, client);
        }

        return this.registryClient.get(clusterId);
    }

    private final Map<String, Map<String, KafkaConnectClient>> connectRestClient = new HashMap<>();

    public Map<String, KafkaConnectClient> getConnectRestClient(String clusterId) throws InvalidClusterException {
        if (!this.clusterExists(clusterId)) {
            throw new InvalidClusterException("Invalid cluster '" + clusterId + "'");
        }

        if (!this.connectRestClient.containsKey(clusterId)) {
            Connection connection = this.getConnection(clusterId);

            if (connection.getConnect() != null && !connection.getConnect().isEmpty()) {

                Map<String, KafkaConnectClient> mapConnects = new HashMap<>();
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
