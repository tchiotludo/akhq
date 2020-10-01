package org.akhq.utils.avroserdes;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroToJsonSerializer {

    public static String toJson(GenericRecord record) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(record.getSchema(), outputStream);
            new BigDecimalFriendlyGenericDatumWriter<GenericRecord>(record.getSchema()).write(record, jsonEncoder);
            jsonEncoder.flush();
            return new String(outputStream.toByteArray());
        }
    }
}

