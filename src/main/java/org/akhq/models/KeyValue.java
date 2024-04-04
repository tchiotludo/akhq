package org.akhq.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a simple key-value pair of any type
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class KeyValue<K,V> {
    K key;
    V value;
}
