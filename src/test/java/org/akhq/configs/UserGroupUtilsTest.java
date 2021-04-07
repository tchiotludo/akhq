package org.akhq.configs;

import io.micronaut.context.ApplicationContext;
import org.akhq.utils.UserGroupUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class UserGroupUtilsTest {
    @Test
    void testTopicRegexpAsString() {
        ApplicationContext ctx = ApplicationContext.run(ApplicationContext.class,"filterregex");

        UserGroupUtils userGroupUtils = ctx.getBean(UserGroupUtils.class);

        List<String> actual = (List<String>)userGroupUtils.getUserAttributes(List.of("as-string")).get("topicsFilterRegexp");

        assertEquals(
                1,
                actual.size()
        );
        assertIterableEquals(
                List.of("test.*"),
                actual
        );

        ctx.close();
    }

    @Test
    void testTopicRegexpAsList() {

        ApplicationContext ctx = ApplicationContext.run(ApplicationContext.class,"filterregex");

        UserGroupUtils userGroupUtils = ctx.getBean(UserGroupUtils.class);

        List<String> actual = (List<String>)userGroupUtils.getUserAttributes(List.of("as-list")).get("topicsFilterRegexp");

        assertEquals(
                2,
                actual.size()
        );
        assertIterableEquals(
                List.of("item1","item2"),
                actual
        );

        ctx.close();
    }
    @Test
    void testTopicRegexpAsMixed() {

        ApplicationContext ctx = ApplicationContext.run(ApplicationContext.class,"filterregex");

        UserGroupUtils userGroupUtils = ctx.getBean(UserGroupUtils.class);

        List<String> actual = (List<String>)userGroupUtils.getUserAttributes(List.of("as-string","as-list")).get("topicsFilterRegexp");

        assertEquals(
                3,
                actual.size()
        );
        assertIterableEquals(
                List.of("test.*","item1","item2"),
                actual
        );

        ctx.close();
    }
}
