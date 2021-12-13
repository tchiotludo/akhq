package org.akhq;

import io.micronaut.context.annotation.Replaces;
import org.akhq.modules.AbstractKafkaWrapper;

import jakarta.inject.Singleton;

@Singleton
@Replaces(AbstractKafkaWrapper.class)
public class KafkaWrapperTest extends AbstractKafkaWrapper {
}
