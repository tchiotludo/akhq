package org.akhq.mcp.services;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.akhq.mcp.model.GetSchemaResult;
import org.akhq.mcp.model.SearchSchemasResult;
import org.akhq.models.Schema;
import org.akhq.repositories.SchemaRegistryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AkhqSchemaToolServiceTest {
    private final SchemaRegistryRepository repository = mock(SchemaRegistryRepository.class);
    private final AkhqSchemaToolService service = new AkhqSchemaToolService(repository);

    @Test
    void searchBoundsTheSubjects() throws Exception {
        when(repository.all("local", Optional.of("orders"), List.of())).thenReturn(List.of("orders-key", "orders-value"));

        SearchSchemasResult result = service.searchSchemas("local", List.of(), "orders", 1);

        assertEquals(2, result.totalMatches());
        assertTrue(result.truncated());
        assertEquals(List.of("orders-key"), result.subjects());
    }

    @Test
    void getSchemaDefaultsToTheLatestVersionAndListsAllVersions() throws Exception {
        when(repository.getAllVersions("local", "orders-value")).thenReturn(List.of(
            new Schema(10, "orders-value", 1), new Schema(12, "orders-value", 3), new Schema(11, "orders-value", 2)
        ));

        GetSchemaResult latest = service.getSchema("local", "orders-value", null);
        GetSchemaResult second = service.getSchema("local", "orders-value", 2);

        assertEquals(3, latest.version());
        assertEquals(12, latest.id());
        assertEquals(List.of(1, 2, 3), latest.versions());
        assertEquals(2, second.version());
        assertFalse(second.schemaTruncated());
    }

    @Test
    void getSchemaReportsUnknownVersionAndSubject() throws Exception {
        when(repository.getAllVersions("local", "orders-value")).thenReturn(List.of(new Schema(10, "orders-value", 1)));
        when(repository.getAllVersions("local", "missing"))
            .thenThrow(new RestClientException("not found", 404, SchemaRegistryRepository.ERROR_NOT_FOUND));

        assertThrows(IllegalArgumentException.class, () -> service.getSchema("local", "orders-value", 9));
        assertThrows(IllegalArgumentException.class, () -> service.getSchema("local", "orders-value", 0));
        assertThrows(IllegalArgumentException.class, () -> service.getSchema("local", "missing", null));
    }

    @Test
    void findSchemasForTopicOnlyUsesVisibleSubjects() throws Exception {
        when(repository.all("local", Optional.of("orders"), List.of("orders.*")))
            .thenReturn(List.of("orders-value", "orders-extra"));
        when(repository.getAllVersions("local", "orders-value")).thenReturn(List.of(new Schema(12, "orders-value", 1)));

        var result = service.findSchemasForTopic("local", List.of("orders.*"), "orders");

        assertEquals(null, result.keySchema());
        assertEquals(12, result.valueSchema().id());
        assertEquals(List.of("orders-extra"), result.otherSubjects());
    }

    @Test
    void getSchemaByIdHidesSubjectsTheCallerCannotSee() throws Exception {
        when(repository.getSubjectsBySchemaId("local", 12))
            .thenReturn(List.of(new Schema(12, "secret-value", 1), new Schema(12, "orders-value", 3)));
        when(repository.all("local", Optional.empty(), List.of("orders.*"))).thenReturn(List.of("orders-value"));
        when(repository.getAllVersions("local", "orders-value")).thenReturn(List.of(new Schema(12, "orders-value", 3)));

        var result = service.getSchemaById("local", List.of("orders.*"), 12, null);

        assertEquals(1, result.subjects().size());
        assertEquals("orders-value", result.schema().subject());
    }

    @Test
    void getSchemaByIdReportsUnknownId() throws Exception {
        when(repository.getSubjectsBySchemaId("local", 99)).thenThrow(new RestClientException("nf", 404, 40403));

        var result = service.getSchemaById("local", List.of(), 99, null);

        assertTrue(result.subjects().isEmpty());
        assertEquals(null, result.schema());
    }
}
