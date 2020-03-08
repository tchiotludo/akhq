package org.akhq;

import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import org.akhq.modules.AbstractKafkaWrapper;

@Prototype
@Replaces(AbstractKafkaWrapper.class)
public class KafkaWrapperTest extends AbstractKafkaWrapper {
}
