package org.akhq.utils;

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
import java.time.temporal.ChronoUnit;

import java.time.Instant;
import java.time.LocalDate;

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

class BigDecimalFriendlySpecificDatumWriter<T> extends SpecificDatumWriter<T> {

    private static final Conversion<BigDecimal> DECIMAL_CONVERSION = new Conversions.DecimalConversion();

    public BigDecimalFriendlySpecificDatumWriter(Schema schema) {
        super(schema);
    }

    @Override
    protected void writeField(Object datum, Schema.Field f, Encoder out, Object state) throws IOException {
        if (datum instanceof GenericData.Record) {
            Schema fieldSchema = f.schema();
            LogicalType logicalType = fieldSchema.getLogicalType();
            Object value = getData().getField(datum, f.name(), f.pos());
            if (logicalType instanceof LogicalTypes.Decimal) {
                value = convert(DECIMAL_CONVERSION, fieldSchema, logicalType, value);
            }
            if (logicalType instanceof LogicalTypes.TimestampMillis) { //to manage "logicalType": "timestamp-millis"
                value= ChronoUnit.MICROS.between(Instant.EPOCH, (Instant)value);
            }
            if (logicalType instanceof LogicalTypes.Date) {
                value= ((LocalDate)value).toEpochDay();
            }
            writeWithoutConversion(fieldSchema, value, out);
        } else {
            super.writeField(datum, f, out, state);
        }
    }

    private Object convert(Conversion<?> conversion, Schema fieldSchema, LogicalType logicalType, Object value) {
        if (conversion instanceof Conversions.DecimalConversion && value instanceof ByteBuffer) {
            // convert decimal value to a string
            byte[] byteValue = new byte[((ByteBuffer) value).remaining()];
            ((ByteBuffer) value).get(byteValue);
            BigDecimal number = (BigDecimal) conversion.fromBytes(ByteBuffer.wrap(byteValue), fieldSchema, logicalType);
            return (ByteBuffer.wrap(number.toPlainString().getBytes()));
        } else {
            return convert(fieldSchema, logicalType, conversion, value);
        }
    }
}
