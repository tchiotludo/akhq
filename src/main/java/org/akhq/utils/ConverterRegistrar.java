package org.akhq.utils;

import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.TypeConverterRegistrar;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import jakarta.inject.Singleton;

@Singleton
public class ConverterRegistrar implements TypeConverterRegistrar {
    @Override
    public void register(ConversionService<?> conversionService) {
        // CharSequence -> Instant
        conversionService.addConverter(
                CharSequence.class,
                Instant.class,
                (object, targetType, context) -> {
                    try {
                        Instant result = Instant.parse(object);
                        return Optional.of(result);
                    } catch (DateTimeParseException e) {
                        context.reject(object, e);
                        return Optional.empty();
                    }
                }
        );
    }
}
