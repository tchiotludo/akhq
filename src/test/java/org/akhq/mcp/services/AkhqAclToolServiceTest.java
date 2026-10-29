package org.akhq.mcp.services;

import org.akhq.mcp.model.GetPrincipalAclsResult;
import org.akhq.mcp.model.SearchAclsResult;
import org.akhq.models.AccessControl;
import org.akhq.repositories.AccessControlListRepository;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AkhqAclToolServiceTest {
    private final AccessControlListRepository repository = mock(AccessControlListRepository.class);
    private final AkhqAclToolService service = new AkhqAclToolService(repository);

    private static AclBinding binding(String principal, ResourceType type, String name, AclOperation operation) {
        return new AclBinding(
            new ResourcePattern(type, name, PatternType.LITERAL),
            new AccessControlEntry(principal, "*", operation, AclPermissionType.ALLOW)
        );
    }

    @Test
    void searchSortsPrincipalsAndBoundsTheResult() throws Exception {
        when(repository.findAll(eq("local"), eq(Optional.of("user")), eq(List.of("User:.*")))).thenReturn(List.of(
            new AccessControl("User:bob", List.of(binding("User:bob", ResourceType.TOPIC, "t", AclOperation.READ))),
            new AccessControl("User:alice", List.of(
                binding("User:alice", ResourceType.TOPIC, "t", AclOperation.READ),
                binding("User:alice", ResourceType.GROUP, "g", AclOperation.READ)
            ))
        ));

        SearchAclsResult result = service.searchAcls("local", List.of("User:.*"), " user ", 1);

        assertEquals(2, result.totalMatches());
        assertTrue(result.truncated());
        assertEquals(List.of(new SearchAclsResult.PrincipalOverview("User:alice", 2)), result.principals());
    }

    @Test
    void principalAclsEncodeThePrincipalSortAndFlattenTheAcls() throws Exception {
        when(repository.findByPrincipal(eq("local"), eq(AccessControl.encodePrincipal("User:alice")), eq(Optional.of(ResourceType.TOPIC))))
            .thenReturn(new AccessControl("User:alice", List.of(
                binding("User:alice", ResourceType.TOPIC, "b", AclOperation.WRITE),
                binding("User:alice", ResourceType.TOPIC, "a", AclOperation.READ)
            )));

        GetPrincipalAclsResult result = service.getPrincipalAcls("local", "User:alice", "topic", null);

        assertFalse(result.truncated());
        assertEquals(2, result.totalAcls());
        assertEquals("a", result.acls().get(0).resourceName());
        assertEquals("READ", result.acls().get(0).operation());
        assertEquals("ALLOW", result.acls().get(0).permissionType());
        assertEquals("LITERAL", result.acls().get(0).patternType());
    }

    @Test
    void principalWithoutAclReturnsAnEmptyList() throws Exception {
        when(repository.findByPrincipal(eq("local"), any(), eq(Optional.empty())))
            .thenReturn(new AccessControl("User:nobody", null));

        GetPrincipalAclsResult result = service.getPrincipalAcls("local", "User:nobody", null, null);

        assertEquals(List.of(), result.acls());
        verify(repository).findByPrincipal("local", AccessControl.encodePrincipal("User:nobody"), Optional.empty());
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> service.getPrincipalAcls("local", "User:a", "ANY", null));
        assertThrows(IllegalArgumentException.class, () -> service.getPrincipalAcls("local", "User:a", "nope", null));
        assertThrows(IllegalArgumentException.class, () -> service.searchAcls("local", List.of(), null, 0));
    }
}
