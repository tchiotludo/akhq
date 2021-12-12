package org.akhq.modules;


import io.micronaut.security.authentication.*;
import io.micronaut.security.ldap.LdapAuthenticationProvider;
import io.micronaut.security.ldap.context.*;
import io.micronaut.security.ldap.group.DefaultLdapGroupProcessor;
import io.micronaut.security.ldap.group.LdapGroupProcessor;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.*;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@MicronautTest(propertySources = "application.yml")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LdapAuthenticationProviderTest {
    @Inject
    ContextBuilder contextBuilder;

    @Inject
    LdapSearchService ldapSearchService;

    @Inject
    LdapGroupProcessor ldapGroupProcessor;

    @Inject
    LdapAuthenticationProvider ldapAuthenticationProvider;

    @MockBean(DefaultContextBuilder.class)
    ContextBuilder contextBuilder() {
        return mock(ContextBuilder.class);
    }

    @Named("default")
    @MockBean(DefaultLdapSearchService.class)
    LdapSearchService ldapSearchService() {
        return mock(LdapSearchService.class);
    }

    @MockBean(DefaultLdapGroupProcessor.class)
    LdapGroupProcessor ldapGroupProcessor() {
        return mock(LdapGroupProcessor.class);
    }

    @Test
    void success() throws NamingException {
        Optional<LdapSearchResult> optionalResult = Optional.of(new LdapSearchResult(new BasicAttributes(), "dn"));
        List<LdapSearchResult> listResults = Collections.singletonList(new LdapSearchResult(new BasicAttributes(), "dn"));

        when(contextBuilder.build(any(ContextSettings.class))).thenReturn(new InitialLdapContext());

        when(ldapSearchService.searchFirst(any(DirContext.class), any(SearchSettings.class))).thenReturn(optionalResult);
        when(ldapSearchService.search(any(DirContext.class), any(SearchSettings.class))).thenReturn(listResults);

        when(ldapGroupProcessor.process(anyString(), any(LdapSearchResult.class), any(SearchProvider.class))).thenReturn(new HashSet<>(Collections.singletonList("ldap-admin")));

        AuthenticationResponse response = Flowable
                .fromPublisher(ldapAuthenticationProvider.authenticate(null, new UsernamePasswordCredentials(
                        "user",
                        "pass"
                ))).blockingFirst();


        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user", response.getAuthentication().get().getName());

        Collection<String> roles = response.getAuthentication().get().getRoles();

        assertThat(roles, hasSize(4));
        assertThat(roles, hasItem("topic/read"));
        assertThat(roles, hasItem("registry/version/delete"));

        assertEquals("test.*", ((List<?>) response.getAuthentication().get().getAttributes().get("topicsFilterRegexp")).get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    void successWithMultipleLdapGroups() throws NamingException {

        Optional<LdapSearchResult> optionalResult = Optional.of(new LdapSearchResult(new BasicAttributes(), "dn"));
        List<LdapSearchResult> listResults = Collections.singletonList(new LdapSearchResult(new BasicAttributes(), "dn"));

        when(contextBuilder.build(any(ContextSettings.class))).thenReturn(new InitialLdapContext());

        when(ldapSearchService.searchFirst(any(DirContext.class), any(SearchSettings.class))).thenReturn(optionalResult);
        when(ldapSearchService.search(any(DirContext.class), any(SearchSettings.class))).thenReturn(listResults);

        when(ldapGroupProcessor.process(anyString(), any(LdapSearchResult.class), any(SearchProvider.class))).thenReturn(new HashSet<>(Arrays.asList("ldap-admin", "ldap-operator")));

        AuthenticationResponse response = Flowable
                .fromPublisher(ldapAuthenticationProvider.authenticate(null, new UsernamePasswordCredentials(
                        "user",
                        "pass"
                ))).blockingFirst();

        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user", response.getAuthentication().get().getName());

        Collection<String> roles = response.getAuthentication().get().getRoles();

        assertThat(roles, hasSize(7));
        assertThat(roles, hasItem("topic/read"));
        assertThat(roles, hasItem("registry/version/delete"));
        assertThat(roles, hasItem("topic/data/read"));

        List<String> topicsFilterList =  (List<String>) (response.getAuthentication().get().getAttributes().get("topicsFilterRegexp"));
        assertThat(topicsFilterList, hasSize(2));
        assertThat(topicsFilterList, hasItem("test.*"));
        assertThat(topicsFilterList, hasItem("test-operator.*"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void successWithLdapGroupAndUserRole() throws NamingException {
        Optional<LdapSearchResult> optionalResult = Optional.of(new LdapSearchResult(new BasicAttributes(), "dn"));
        List<LdapSearchResult> listResults = Collections.singletonList(new LdapSearchResult(new BasicAttributes(), "dn"));

        when(contextBuilder.build(any(ContextSettings.class))).thenReturn(new InitialLdapContext());

        when(ldapSearchService.searchFirst(any(DirContext.class), any(SearchSettings.class))).thenReturn(optionalResult);
        when(ldapSearchService.search(any(DirContext.class), any(SearchSettings.class))).thenReturn(listResults);

        when(ldapGroupProcessor.process(anyString(), any(LdapSearchResult.class), any(SearchProvider.class))).thenReturn(new HashSet<>(List.of("ldap-admin")));

        AuthenticationResponse response = Flowable
                        .fromPublisher(ldapAuthenticationProvider.authenticate(null, new UsernamePasswordCredentials(
                                        "user2",
                                        "pass"
                        ))).blockingFirst();

        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user2", response.getAuthentication().get().getName());

        Collection<String> roles = response.getAuthentication().get().getRoles();

        assertThat(roles, hasSize(7));
        assertThat(roles, hasItem("topic/read"));
        assertThat(roles, hasItem("registry/version/delete"));
        assertThat(roles, hasItem("topic/data/read"));

        List<String> topicsFilterList =  (List<String>) (response.getAuthentication().get().getAttributes().get("topicsFilterRegexp"));
        assertThat(topicsFilterList, hasSize(2));
        assertThat(topicsFilterList, hasItem("test.*"));
        assertThat(topicsFilterList, hasItem("test-operator.*"));
    }

    @Test
    void successWithoutRoles() throws NamingException {
        Optional<LdapSearchResult> optionalResult = Optional.of(new LdapSearchResult(new BasicAttributes(), "dn"));
        List<LdapSearchResult> listResults = Collections.singletonList(new LdapSearchResult(new BasicAttributes(), "dn"));

        when(contextBuilder.build(any(ContextSettings.class))).thenReturn(new InitialLdapContext());

        when(ldapSearchService.searchFirst(any(DirContext.class), any(SearchSettings.class))).thenReturn(optionalResult);
        when(ldapSearchService.search(any(DirContext.class), any(SearchSettings.class))).thenReturn(listResults);

        when(ldapGroupProcessor.process(anyString(), any(LdapSearchResult.class), any(SearchProvider.class))).thenReturn(new HashSet<>(Collections.singletonList(("ldap-other-group"))));

        AuthenticationResponse response = Flowable
                .fromPublisher(ldapAuthenticationProvider.authenticate(null, new UsernamePasswordCredentials(
                        "user",
                        "pass"
                ))).blockingFirst();

        assertTrue(response.isAuthenticated());
        assertTrue(response.getAuthentication().isPresent());
        assertEquals("user", response.getAuthentication().get().getName());

        Collection<String> roles = response.getAuthentication().get().getRoles();
        assertThat(roles, hasSize(0));
    }

    @Test
    void failure() throws NamingException {
        Optional<LdapSearchResult> optionalResult = Optional.empty();

        when(contextBuilder.build(any(ContextSettings.class))).thenReturn(new InitialLdapContext());

        when(ldapSearchService.searchFirst(any(DirContext.class), any(SearchSettings.class))).thenReturn(optionalResult);

        AuthenticationException authenticationException = assertThrows(AuthenticationException.class, () -> {
            Flowable
                .fromPublisher(ldapAuthenticationProvider.authenticate(null, new UsernamePasswordCredentials(
                    "user",
                    "pass"
                ))).blockingFirst();
        });

        assertThat(authenticationException.getResponse(), instanceOf(AuthenticationFailed.class));
        assertNotNull(authenticationException.getResponse());
        assertFalse(authenticationException.getResponse().isAuthenticated());
    }
}
