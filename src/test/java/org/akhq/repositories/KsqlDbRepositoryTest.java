package org.akhq.repositories;

import jakarta.inject.Inject;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.*;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class KsqlDbRepositoryTest extends AbstractTest {

    @Inject
    @InjectMocks
    private KsqlDbRepository repository;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterEach
    void cleanup() {
        try {
            repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb",
                "DROP TABLE IF EXISTS ORDERS_BY_USERS DELETE TOPIC;");
        } catch (Exception ignored) {
        }
        try {
            repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb",
                "DROP TABLE IF EXISTS USERS_BY_USERS DELETE TOPIC;");
        } catch (Exception ignored) {
        }
        try {
            repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb",
                "DROP STREAM IF EXISTS ORDERS DELETE TOPIC;");
        } catch (Exception ignored) {
        }
        try {
            repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb",
                "DROP STREAM IF EXISTS USERS DELETE TOPIC;");
        } catch (Exception ignored) {
        }
        try {
            repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb",
                "DROP TABLE IF EXISTS USERS DELETE TOPIC;");
        } catch (Exception ignored) {
        }
        try {
            repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb",
                "DROP TABLE IF EXISTS ORDERS DELETE TOPIC;");
        } catch (Exception ignored) {
        }
    }

    @Test
    void getServerInfo() {
        KsqlDbServerInfo serverInfo = repository.getServerInfo(KafkaTestCluster.CLUSTER_ID, "ksqldb");
        assertNotNull(serverInfo.getKafkaClusterId());
        assertEquals("7.3.3", serverInfo.getServerVersion());
        assertEquals("ksql", serverInfo.getKsqlServiceId());
    }

    @Test
    void listStreams() {
        List<KsqlDbStream> ksqlDbStreams = repository.listStreams(KafkaTestCluster.CLUSTER_ID, "ksqldb");
        assertEquals(0, ksqlDbStreams.size());
    }

    @Test
    void getPaginatedStreams() throws ExecutionException, InterruptedException {
        final String sql = "CREATE STREAM ORDERS (ORDER_ID BIGINT, PRODUCT_ID VARCHAR, USER_ID VARCHAR) WITH (KAFKA_TOPIC='orders', PARTITIONS=1, VALUE_FORMAT='json');";
        assertDoesNotThrow(() -> repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sql));
        final String sql2 = "CREATE STREAM USERS (ORDER_ID BIGINT, PRODUCT_ID VARCHAR, USER_ID VARCHAR) WITH (KAFKA_TOPIC='users', PARTITIONS=1, VALUE_FORMAT='json');";
        assertDoesNotThrow(() -> repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sql2));

        PagedList<KsqlDbStream> paginatedStreams = repository.getPaginatedStreams(KafkaTestCluster.CLUSTER_ID, "ksqldb",
            new Pagination(25, URIBuilder.empty(), 1), Optional.of("USER"));
        assertEquals(1, paginatedStreams.size());
    }

    @Test
    void listTables() {
        List<KsqlDbTable> ksqlDbTables = repository.listTables(KafkaTestCluster.CLUSTER_ID, "ksqldb");
        assertEquals(0, ksqlDbTables.size());
    }

    @Test
    void getPaginatedTables() throws ExecutionException, InterruptedException {
        final String sql = "CREATE TABLE ORDERS (ORDER_ID BIGINT PRIMARY KEY, PRODUCT_ID VARCHAR, USER_ID VARCHAR) WITH (KAFKA_TOPIC='orders', PARTITIONS=1, VALUE_FORMAT='json');";
        assertDoesNotThrow(() -> repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sql));
        final String sql2 = "CREATE TABLE USERS (ORDER_ID BIGINT PRIMARY KEY, PRODUCT_ID VARCHAR, USER_ID VARCHAR) WITH (KAFKA_TOPIC='users', PARTITIONS=1, VALUE_FORMAT='json');";
        assertDoesNotThrow(() -> repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sql2));

        PagedList<KsqlDbTable> paginatedTables = repository.getPaginatedTables(KafkaTestCluster.CLUSTER_ID, "ksqldb",
            new Pagination(25, URIBuilder.empty(), 1), Optional.of("USER"));
        assertEquals(1, paginatedTables.size());
    }

    @Test
    void listQueries() {
        List<KsqlDbQuery> ksqlDbQueries = repository.listQueries(KafkaTestCluster.CLUSTER_ID, "ksqldb");
        assertEquals(0, ksqlDbQueries.size());
    }

    @Test
    void getPaginatedQueries() throws ExecutionException, InterruptedException {
        final String sqlStream = "CREATE STREAM ORDERS (ORDER_ID BIGINT, PRODUCT_ID VARCHAR, USER_ID VARCHAR) WITH (KAFKA_TOPIC='orders', PARTITIONS=1, VALUE_FORMAT='json');";
        repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sqlStream);
        final String sql = "CREATE TABLE ORDERS_BY_USERS AS SELECT USER_ID, COUNT(*) as COUNT FROM ORDERS GROUP BY USER_ID EMIT CHANGES;";
        assertDoesNotThrow(() -> repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sql));
        final String sql2 = "CREATE TABLE USERS_BY_USERS AS SELECT USER_ID, COUNT(*) as COUNT FROM ORDERS GROUP BY USER_ID EMIT CHANGES;";
        assertDoesNotThrow(() -> repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sql2));

        PagedList<KsqlDbQuery> paginatedQueries = repository.getPaginatedQueries(KafkaTestCluster.CLUSTER_ID, "ksqldb",
            new Pagination(25, URIBuilder.empty(), 1), Optional.of("ORDERS"));
        assertEquals(1, paginatedQueries.size());
    }

    @Test
    void executeStatement_stream() {
        final String sql = "CREATE STREAM ORDERS (ORDER_ID BIGINT, PRODUCT_ID VARCHAR, USER_ID VARCHAR) WITH (KAFKA_TOPIC='orders', PARTITIONS=1, VALUE_FORMAT='json');";
        assertDoesNotThrow(() -> repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sql));

        List<KsqlDbStream> ksqlDbStreams = repository.listStreams(KafkaTestCluster.CLUSTER_ID, "ksqldb");
        assertEquals(1, ksqlDbStreams.size());
    }

    @Test
    void executeStatement_table() {
        final String sqlStream = "CREATE STREAM ORDERS (ORDER_ID BIGINT, PRODUCT_ID VARCHAR, USER_ID VARCHAR) WITH (KAFKA_TOPIC='orders', PARTITIONS=1, VALUE_FORMAT='json');";
        repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sqlStream);
        final String sql = "CREATE TABLE ORDERS_BY_USERS AS SELECT USER_ID, COUNT(*) as COUNT FROM ORDERS GROUP BY USER_ID EMIT CHANGES;";
        assertDoesNotThrow(() -> repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sql));

        List<KsqlDbTable> ksqlDbTables = repository.listTables(KafkaTestCluster.CLUSTER_ID, "ksqldb");
        assertEquals(1, ksqlDbTables.size());

        List<KsqlDbQuery> ksqlDbQueries = repository.listQueries(KafkaTestCluster.CLUSTER_ID, "ksqldb");
        assertEquals(1, ksqlDbQueries.size());
    }

    @Test
    void executeQuery() {
        final String sqlStream = "CREATE STREAM ORDERS (ORDER_ID BIGINT, PRODUCT_ID VARCHAR, USER_ID VARCHAR) WITH (KAFKA_TOPIC='orders', PARTITIONS=1, VALUE_FORMAT='json');";
        repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sqlStream);
        repository.insertRow(KafkaTestCluster.CLUSTER_ID, "ksqldb", "ORDERS", Map.of("ORDER_ID",1, "PRODUCT_ID", "1", "USER_ID", "1"));

        final String sql = "SELECT * FROM ORDERS LIMIT 5;";
        KsqlDbQueryResult ksqlDbQueryResult = repository.executeQuery(KafkaTestCluster.CLUSTER_ID, "ksqldb", sql, Collections.singletonMap("auto.offset.reset", "earliest"));
        assertEquals(3, ksqlDbQueryResult.getColumnNames().size());
        assertEquals(1, ksqlDbQueryResult.getColumnValues().size());
        assertEquals(List.of("ORDER_ID", "PRODUCT_ID", "USER_ID"), ksqlDbQueryResult.getColumnNames());
        assertEquals("[1,\"1\",\"1\"]", ksqlDbQueryResult.getColumnValues().get(0));
    }

    @Test
    void executeQuery_noResult() {
        final String sqlStream = "CREATE STREAM ORDERS (ORDER_ID BIGINT, PRODUCT_ID VARCHAR, USER_ID VARCHAR) WITH (KAFKA_TOPIC='orders', PARTITIONS=1, VALUE_FORMAT='json');";
        repository.executeStatement(KafkaTestCluster.CLUSTER_ID, "ksqldb", sqlStream);

        final String sql = "SELECT * FROM ORDERS LIMIT 5;";
        KsqlDbQueryResult ksqlDbQueryResult = repository.executeQuery(KafkaTestCluster.CLUSTER_ID, "ksqldb", sql, Collections.singletonMap("auto.offset.reset", "earliest"));
        assertEquals(0, ksqlDbQueryResult.getColumnNames().size());
        assertEquals(0, ksqlDbQueryResult.getColumnValues().size());
    }
}