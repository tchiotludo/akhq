package org.akhq.controllers;

import com.google.common.collect.ImmutableMap;
import io.micronaut.http.HttpRequest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.*;
import org.akhq.utils.ResultPagedList;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KsqlDbControllerTest extends AbstractTest {

    public static final String BASE_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/ksqldb/ksqldb";

    @Test
    @Order(1)
    void info() {
        KsqlDbServerInfo serverInfo = this.retrieve(HttpRequest.GET(BASE_URL + "/info"), KsqlDbServerInfo.class);
        assertNotNull(serverInfo.getKafkaClusterId());
        assertEquals("7.3.3", serverInfo.getServerVersion());
        assertEquals("ksql", serverInfo.getKsqlServiceId());
    }

    @Test
    @Order(1)
    void executeStatement() {
        this.exchange(HttpRequest.PUT(BASE_URL + "/execute", ImmutableMap.of(
            "sql", "CREATE STREAM ORDERS (ORDER_ID BIGINT, PRODUCT_ID VARCHAR, USER_ID VARCHAR) WITH (KAFKA_TOPIC='orders', PARTITIONS=1, VALUE_FORMAT='json');"
        )));
        this.exchange(HttpRequest.PUT(BASE_URL + "/execute", ImmutableMap.of(
            "sql", "CREATE TABLE ORDERS_BY_USERS AS SELECT USER_ID, COUNT(*) as COUNT FROM ORDERS GROUP BY USER_ID EMIT CHANGES;"
        )));
    }

    @Test
    @Order(2)
    void listStreams() {
        ResultPagedList<KsqlDbStream> ksqlDbStreamResultPagedList = this.retrievePagedList(HttpRequest.GET(BASE_URL + "/streams"), KsqlDbStream.class);
        assertEquals(1, ksqlDbStreamResultPagedList.getResults().size());
    }

    @Test
    @Order(2)
    void listTables() {
        ResultPagedList<KsqlDbTable> ksqlDbTableResultPagedList = this.retrievePagedList(HttpRequest.GET(BASE_URL + "/tables"), KsqlDbTable.class);
        assertEquals(1, ksqlDbTableResultPagedList.getResults().size());
    }

    @Test
    @Order(2)
    void listQueries() {
        ResultPagedList<KsqlDbQuery> ksqlDbQueryResultPagedList = this.retrievePagedList(HttpRequest.GET(BASE_URL + "/queries"), KsqlDbQuery.class);
        assertEquals(1, ksqlDbQueryResultPagedList.getResults().size());
    }

    @Test
    @Order(2)
    void pullQuery() {
        KsqlDbQueryResult ksqlDbQueryResult = this.retrieve(HttpRequest.PUT(BASE_URL + "/queries/pull", ImmutableMap.of(
            "sql", "SELECT * FROM ORDERS LIMIT 5;",
            "properties", ImmutableMap.of(
                "auto.offset.reset", "earliest"
            )
        )), KsqlDbQueryResult.class);
        assertNotNull(ksqlDbQueryResult);
    }

    @Test
    @Order(3)
    void cleanup() {
        try {
            this.exchange(HttpRequest.PUT(BASE_URL + "/execute", ImmutableMap.of(
                "sql", "DROP TABLE IF EXISTS ORDERS_BY_USERS DELETE TOPIC;"
            )));
        } catch (Exception ignored) {
        }
        try {
            this.exchange(HttpRequest.PUT(BASE_URL + "/execute", ImmutableMap.of(
                "sql", "DROP STREAM IF EXISTS ORDERS DELETE TOPIC;"
            )));
        } catch (Exception ignored) {
        }
    }
}