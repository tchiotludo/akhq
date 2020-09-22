package org.akhq.utils.avroserde;

import org.apache.avro.*;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;

public class AvroToJsonSerializer {

    public static String toJson(GenericRecord record) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(record.getSchema(), outputStream);
            new BigDecimalFriendlySpecificDatumWriter<GenericRecord>(record.getSchema()).write(record, jsonEncoder);
            jsonEncoder.flush();
            return new String(outputStream.toByteArray());
        }
    }
}

