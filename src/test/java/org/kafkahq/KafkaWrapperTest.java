package org.kafkahq;

import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import org.kafkahq.modules.AbstractKafkaWrapper;

@Prototype
@Replaces(AbstractKafkaWrapper.class)
public class KafkaWrapperTest extends AbstractKafkaWrapper {
}
