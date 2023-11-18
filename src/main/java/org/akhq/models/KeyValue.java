package org.akhq.models;

//import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a simple key-value pair of any type
 */
@Getter
@AllArgsConstructor
//@Serdeable
public class KeyValue<K,V> {
    K key;
    V value;
}
