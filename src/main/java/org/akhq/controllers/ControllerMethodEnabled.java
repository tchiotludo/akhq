package org.akhq.controllers;

import io.micronaut.context.annotation.Requires;

import java.lang.annotation.*;

/**
 * Annotation to conditionally enable/disable controller methods based on configuration.
 *
 * Usage:
 * <pre>
 * {@code
 * @ControllerMethodEnabled(property = "akhq.controllers.topic.list.enabled", defaultValue = true)
 * @Get("/api/{cluster}/topic")
 * public ResultPagedList<Topic> list(...) {
 *     ...
 * }
 * }
 * </pre>
 *
 * Configuration:
 * <pre>
 * akhq:
 *   controllers:
 *     topic:
 *       list:
 *         enabled: false
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Requires(condition = ControllerMethodEnabledCondition.class)
public @interface ControllerMethodEnabled {
    /**
     * The property name to check for enabling/disabling the controller method.
     * @return the property name
     */
    String property();

    /**
     * The default value if the property is not set.
     * @return true if the method should be enabled by default, false otherwise
     */
    boolean defaultValue() default true;
}
