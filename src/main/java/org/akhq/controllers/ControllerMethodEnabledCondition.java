package org.akhq.controllers;

import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;

/**
 * Condition that checks if a controller method should be enabled based on configuration.
 * This condition is used by the {@link ControllerMethodEnabled} annotation.
 */
public class ControllerMethodEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context) {
        AnnotationMetadata annotationMetadata = context.getComponent().getAnnotationMetadata();

        AnnotationValue<ControllerMethodEnabled> annotation =
            annotationMetadata.getAnnotation(ControllerMethodEnabled.class);

        if (annotation == null) {
            return true; // If annotation is not present, enable by default
        }

        String property = annotation.stringValue("property").orElse(null);
        boolean defaultValue = annotation.booleanValue("defaultValue").orElse(true);

        if (property == null) {
            return defaultValue;
        }

        // Check if the property exists and get its boolean value
        return context.getProperty(property, Boolean.class)
            .orElse(defaultValue);
    }
}
