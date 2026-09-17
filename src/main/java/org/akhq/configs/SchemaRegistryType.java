package org.akhq.configs;

import lombok.Getter;

@Getter
public enum SchemaRegistryType {
    CONFLUENT((byte) 0x0),
    TIBCO((byte) 0x80),
    GLUE((byte) 0x0),
    BSR((byte) 0x0);  // BSR doesn't use magic byte prefix

    private byte magicByte;

    SchemaRegistryType(byte magicByte) {
        this.magicByte = magicByte;
    }
}
