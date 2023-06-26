package org.akhq.configs.security;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.CollectionUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class SecurityPropertiesTest {

    @Test
    void shouldReturnAllBasicGroups() {
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext.class);
        SecurityProperties securityProperties = ctx.getBean(SecurityProperties.class);

        assertEquals(
                CollectionUtils.toSet(new String[] {"admin", "limited", "operator", "no-filter"}),
                securityProperties.getGroups().keySet()
        );
        ctx.close();
    }

    @Test
    void shouldReturnAllBasicPlusConfiguredGroups() {
        ApplicationContext ctx = ApplicationContext.run("extragroups");
        SecurityProperties securityProperties = ctx.getBean(SecurityProperties.class);

        assertEquals(
                CollectionUtils.toSet(new String[] {"admin", "limited", "operator", "no-filter", "extra", "another"}),
                securityProperties.getGroups().keySet()
        );

        ctx.close();
    }

    @Test
    void shouldOverrideBasicGroups() {
        ApplicationContext ctx = ApplicationContext.run("overridegroups");
        SecurityProperties securityProperties = ctx.getBean(SecurityProperties.class);

        assertEquals(
                CollectionUtils.toSet(new String[] {"admin", "limited", "operator", "no-filter", "extra"}),
                securityProperties.getGroups().keySet()
        );

        assertThat(securityProperties.getGroups().get("admin"), hasSize(1));
        assertThat(securityProperties.getGroups().get("admin").get(0).getRole(), is("topic-read"));
        assertThat(securityProperties.getGroups().get("admin").get(0).getPatterns(), containsInAnyOrder(".*"));

        ctx.close();
    }

}
