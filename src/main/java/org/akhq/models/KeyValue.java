package org.akhq.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a simple key-value pair of any type
 */
@Getter
@AllArgsConstructor
public class KeyValue<K,V> {
    K key;
    V value;
}
