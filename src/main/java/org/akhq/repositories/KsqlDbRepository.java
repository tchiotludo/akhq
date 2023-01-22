package org.akhq.repositories;

import io.confluent.ksql.api.client.KsqlObject;
import io.confluent.ksql.api.client.Row;
import io.confluent.ksql.api.client.ServerInfo;
import io.micronaut.retry.annotation.Retryable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.akhq.models.*;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.PagedList;
import org.akhq.utils.Pagination;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.ConcurrentConfigModificationException;
import org.sourcelab.kafka.connect.apiclient.rest.exceptions.InvalidRequestException;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class KsqlDbRepository extends AbstractRepository {
    @Inject
    private KafkaModule kafkaModule;

    public KsqlDbServerInfo getServerInfo(String clusterId, String ksqlDbId) {
        try {
            ServerInfo serverInfo = this.kafkaModule
                .getKsqlDbClient(clusterId)
                .get(ksqlDbId)
                .serverInfo().get();
            return new KsqlDbServerInfo(
                serverInfo.getServerVersion(),
                serverInfo.getKafkaClusterId(),
                serverInfo.getKsqlServiceId()
            );
        } catch (InvalidRequestException | InterruptedException | ExecutionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Retryable(includes = {
        ConcurrentConfigModificationException.class
    }, delay = "3s", attempts = "5")
    public PagedList<KsqlDbStream> getPaginatedStreams(String clusterId, String connectId, Pagination pagination, Optional<String> search)
        throws ExecutionException, InterruptedException{
        List<KsqlDbStream> ksqlDbStreams = listStreams(clusterId, connectId);

        List<KsqlDbStream> ksqlDbStreamsFilteredBySearch = search.map(
            query -> ksqlDbStreams.stream().filter(stream -> stream.getName().contains(query))
        ).orElse(ksqlDbStreams.stream()).collect(Collectors.toList());

        return PagedList.of(ksqlDbStreamsFilteredBySearch, pagination, list -> list);
    }

    public List<KsqlDbStream> listStreams(String clusterId, String ksqlDbId) {
        try {
            return this.kafkaModule
                .getKsqlDbClient(clusterId)
                .get(ksqlDbId)
                .listStreams()
                .get()
                .stream().map(streamInfo -> new KsqlDbStream(
                    streamInfo.getName(),
                    streamInfo.getTopic(),
                    streamInfo.getKeyFormat(),
                    streamInfo.getValueFormat(),
                    streamInfo.isWindowed()
                )).collect(Collectors.toList());
        } catch (InvalidRequestException | InterruptedException | ExecutionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Retryable(includes = {
        ConcurrentConfigModificationException.class
    }, delay = "3s", attempts = "5")
    public PagedList<KsqlDbTable> getPaginatedTables(String clusterId, String connectId, Pagination pagination, Optional<String> search)
        throws ExecutionException, InterruptedException{
        List<KsqlDbTable> ksqlDbStreams = listTables(clusterId, connectId);

        List<KsqlDbTable> ksqlDbTablesFilteredBySearch = search.map(
            query -> ksqlDbStreams.stream().filter(table -> table.getName().contains(query))
        ).orElse(ksqlDbStreams.stream()).collect(Collectors.toList());

        return PagedList.of(ksqlDbTablesFilteredBySearch, pagination, list -> list);
    }

    public List<KsqlDbTable> listTables(String clusterId, String ksqlDbId) {
        try {
            return this.kafkaModule
                .getKsqlDbClient(clusterId)
                .get(ksqlDbId)
                .listTables()
                .get()
                .stream().map(tableInfo -> new KsqlDbTable(
                    tableInfo.getName(),
                    tableInfo.getTopic(),
                    tableInfo.getKeyFormat(),
                    tableInfo.getValueFormat(),
                    tableInfo.isWindowed()
                )).collect(Collectors.toList());
        } catch (InvalidRequestException | InterruptedException | ExecutionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Retryable(includes = {
        ConcurrentConfigModificationException.class
    }, delay = "3s", attempts = "5")
    public PagedList<KsqlDbQuery> getPaginatedQueries(String clusterId, String connectId, Pagination pagination, Optional<String> search)
        throws ExecutionException, InterruptedException{
        List<KsqlDbQuery> ksqlDbQueries = listQueries(clusterId, connectId);

        List<KsqlDbQuery> ksqlDbQueriesFilteredBySearch = search.map(
            query -> ksqlDbQueries.stream().filter(ksqlDbQuery -> ksqlDbQuery.getId().contains(query))
        ).orElse(ksqlDbQueries.stream()).collect(Collectors.toList());

        return PagedList.of(ksqlDbQueriesFilteredBySearch, pagination, list -> list);
    }

    public List<KsqlDbQuery> listQueries(String clusterId, String ksqlDbId) {
        try {
            return this.kafkaModule
                .getKsqlDbClient(clusterId)
                .get(ksqlDbId)
                .listQueries()
                .get()
                .stream().map(queryInfo -> new KsqlDbQuery(
                    queryInfo.getQueryType().name(),
                    queryInfo.getId(),
                    queryInfo.getSql(),
                    queryInfo.getSink().orElse(""),
                    queryInfo.getSinkTopic().orElse("")
                )).collect(Collectors.toList());
        } catch (InvalidRequestException | InterruptedException | ExecutionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void insertRow(String clusterId, String ksqlDbId, String streamName, Map<String, Object> keyValueRow) {
        try {
            KsqlObject row = new KsqlObject();
            keyValueRow.forEach(row::put);

            this.kafkaModule
                .getKsqlDbClient(clusterId)
                .get(ksqlDbId)
                .insertInto(streamName, row)
                .get();
        } catch (InvalidRequestException | InterruptedException | ExecutionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String executeStatement(String clusterId, String ksqlDbId, String sql) {
        try {
            return this.kafkaModule
                .getKsqlDbClient(clusterId)
                .get(ksqlDbId)
                .executeStatement(sql)
                .get().queryId().orElse("");
        } catch (InvalidRequestException | InterruptedException | ExecutionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public KsqlDbQueryResult executeQuery(String clusterId, String ksqlDbId, String sql, Map<String, String> properties) {
        try {
            HashMap<String, Object> stringObjectHashMap = new HashMap<>();
            if (properties != null) {
                stringObjectHashMap.putAll(properties);
            }

            List<Row> rows = this.kafkaModule
                .getKsqlDbClient(clusterId)
                .get(ksqlDbId)
                .executeQuery(sql, stringObjectHashMap)
                .get();
            if (rows.size() == 0) {
                return new KsqlDbQueryResult(new ArrayList<>(), new ArrayList<>());
            }
            return new KsqlDbQueryResult(
                rows.get(0).columnNames(),
                rows.stream().map(row -> row.values().toJsonString()).collect(Collectors.toList())
            );
        } catch (InvalidRequestException | InterruptedException | ExecutionException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
