package org.akhq.configs;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.CollectionUtils;

import java.util.Collections;

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
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext.class, "extragroups");
        SecurityProperties securityProperties = ctx.getBean(SecurityProperties.class);

        assertEquals(
                CollectionUtils.toSet(new String[] {"admin", "limited", "operator", "no-filter", "extra", "another"}),
                securityProperties.getGroups().keySet()
        );

        ctx.close();
    }

    @Test
    void shouldOverrideBasicGroups() {
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext.class, "overridegroups");
        SecurityProperties securityProperties = ctx.getBean(SecurityProperties.class);

        assertEquals(
                CollectionUtils.toSet(new String[] {"admin", "limited", "operator", "no-filter", "extra"}),
                securityProperties.getGroups().keySet()
        );
        assertEquals(
                Collections.singletonList("topic/read"),
                securityProperties.getGroups().get("admin").roles
        );

        ctx.close();
    }

}
