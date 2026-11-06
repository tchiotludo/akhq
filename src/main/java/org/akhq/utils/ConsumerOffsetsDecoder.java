package org.akhq.utils;

import java.nio.ByteBuffer;

import org.apache.kafka.coordinator.group.GroupCoordinatorRecordSerde;
import org.apache.kafka.coordinator.common.runtime.CoordinatorRecord;

public class ConsumerOffsetsDecoder {

    private final GroupCoordinatorRecordSerde serde =
        new GroupCoordinatorRecordSerde();

    public CoordinatorRecord decode(
        byte[] keyBytes,
        byte[] valueBytes
    ) {

        ByteBuffer key =
            ByteBuffer.wrap(keyBytes);

        ByteBuffer value =
            valueBytes == null
                ? null
                : ByteBuffer.wrap(valueBytes);

        return serde.deserialize(key, value);
    }

}
