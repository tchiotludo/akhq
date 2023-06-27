package org.akhq.controllers;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.inject.Inject;
import org.akhq.configs.security.Role;
import org.akhq.models.*;
import org.akhq.repositories.KsqlDbRepository;
import org.akhq.security.annotation.AKHQSecured;
import org.akhq.utils.Pagination;
import org.akhq.utils.ResultPagedList;
import org.codehaus.httpcache4j.uri.URIBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@AKHQSecured(resource = Role.Resource.KSQLDB, action = Role.Action.READ)
@Controller("/api/{cluster}/ksqldb/{ksqlDbId}")
public class KsqlDbController extends AbstractController {
    private final KsqlDbRepository ksqlDbRepository;

    @Value("${akhq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public KsqlDbController(KsqlDbRepository ksqlDbRepository) {
        this.ksqlDbRepository = ksqlDbRepository;
    }

    @Get("/info")
    @Operation(tags = {"ksqlDB"}, summary = "Retrieve server info")
    public KsqlDbServerInfo info(String cluster, String ksqlDbId) {
        return this.ksqlDbRepository.getServerInfo(cluster, ksqlDbId);
    }

    @Get("/streams")
    @Operation(tags = {"ksqlDB"}, summary = "List all streams")
    public ResultPagedList<KsqlDbStream> listStreams(
        HttpRequest<?> request, String cluster, String ksqlDbId,  Optional<String> search, Optional<Integer> page)
        throws ExecutionException, InterruptedException
    {
        checkIfClusterAllowed(cluster);

        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        return ResultPagedList.of(this.ksqlDbRepository.getPaginatedStreams(cluster, ksqlDbId, pagination, search));
    }

    @Get("/tables")
    @Operation(tags = {"ksqlDB"}, summary = "List all tables")
    public ResultPagedList<KsqlDbTable> listTables(
        HttpRequest<?> request, String cluster, String ksqlDbId,  Optional<String> search, Optional<Integer> page)
        throws ExecutionException, InterruptedException
    {
        checkIfClusterAllowed(cluster);

        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        return ResultPagedList.of(this.ksqlDbRepository.getPaginatedTables(cluster, ksqlDbId, pagination, search));
    }

    @Get("/queries")
    @Operation(tags = {"ksqlDB"}, summary = "List all queries")
    public ResultPagedList<KsqlDbQuery> listQueries(
        HttpRequest<?> request, String cluster, String ksqlDbId,  Optional<String> search, Optional<Integer> page)
        throws ExecutionException, InterruptedException
    {
        checkIfClusterAllowed(cluster);

        URIBuilder uri = URIBuilder.fromURI(request.getUri());
        Pagination pagination = new Pagination(pageSize, uri, page.orElse(1));

        return ResultPagedList.of(this.ksqlDbRepository.getPaginatedQueries(cluster, ksqlDbId, pagination, search));
    }

    @AKHQSecured(resource = Role.Resource.KSQLDB, action = Role.Action.EXECUTE)
    @Put("/queries/pull")
    @Operation(tags = {"ksqlDB"}, summary = "Execute a query")
    public KsqlDbQueryResult pullQuery(String cluster, String ksqlDbId, String sql, Map<String, String> properties) {
        checkIfClusterAllowed(cluster);

        return this.ksqlDbRepository.executeQuery(cluster, ksqlDbId, sql, properties);
    }

    @AKHQSecured(resource = Role.Resource.KSQLDB, action = Role.Action.EXECUTE)
    @Put("/execute")
    @Operation(tags = {"ksqlDB"}, summary = "Execute a statement")
    public String executeStatement(String cluster, String ksqlDbId, String sql) {
        checkIfClusterAllowed(cluster);

        return this.ksqlDbRepository.executeStatement(cluster, ksqlDbId, sql);
    }
}
