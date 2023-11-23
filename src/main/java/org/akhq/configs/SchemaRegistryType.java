package org.akhq.configs;

import lombok.Getter;

@Getter
public enum SchemaRegistryType {
    CONFLUENT((byte) 0x0),
    TIBCO((byte) 0x80);

    private byte magicByte;

    SchemaRegistryType(byte magicByte) {
        this.magicByte = magicByte;
    }
}
