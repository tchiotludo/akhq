package org.akhq.configs.security;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@MicronautTest(environments = "test")
class SecurityPropertiesTest {

    @Inject
    private SecurityProperties securityProperties;

    @Test
    void shouldReturnAllBasicGroups() {
        assertEquals(
                Set.of("admin", "limited", "operator", "no-filter"),
                securityProperties.getGroups().keySet()
        );
    }

    @MicronautTest(environments = {"test", "extragroups"})
    static class ExtraGroupsTest {
        @Inject
        private SecurityProperties securityProperties;

        @Test
        void shouldReturnAllBasicPlusConfiguredGroups() {
            assertEquals(
                    Set.of("admin", "limited", "operator", "no-filter", "extra", "another"),
                    securityProperties.getGroups().keySet()
            );
        }
    }

    @MicronautTest(environments = {"test", "overridegroups"})
    static class OverrideGroupsTest {
        @Inject
        private SecurityProperties securityProperties;

        @Test
        void shouldOverrideBasicGroups() {
            assertEquals(
                    Set.of("admin", "limited", "operator", "no-filter", "extra"),
                    securityProperties.getGroups().keySet()
            );

            assertThat(securityProperties.getGroups().get("admin"), hasSize(1));
            assertThat(securityProperties.getGroups().get("admin").get(0).getRole(), is("topic-read"));
            assertThat(securityProperties.getGroups().get("admin").get(0).getPatterns(), containsInAnyOrder(".*"));
        }
    }

}
