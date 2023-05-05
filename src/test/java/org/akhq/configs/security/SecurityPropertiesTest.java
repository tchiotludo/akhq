package org.akhq.configs.security;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.CollectionUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        /* TODO fix
        assertEquals(
                Collections.singletonList("topic/read"),
                securityProperties.getGroups().get("admin").roles
        );
        */

        ctx.close();
    }

}
