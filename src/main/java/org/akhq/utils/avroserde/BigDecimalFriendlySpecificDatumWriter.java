package org.akhq.utils.avroserde;

import org.apache.avro.Conversion;
import org.apache.avro.Conversions;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.Encoder;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;

class BigDecimalFriendlySpecificDatumWriter<T> extends SpecificDatumWriter<T> {

    private static final Conversion<BigDecimal> DECIMAL_CONVERSION = new Conversions.DecimalConversion();

    public BigDecimalFriendlySpecificDatumWriter(Schema schema) {
        super(schema);
    }

    @Override
    protected void writeField(Object datum, Schema.Field f, Encoder out, Object state) throws IOException {
        Schema fieldSchema = f.schema();
        LogicalType logicalType = fieldSchema.getLogicalType();

        if (datum instanceof GenericData.Record && logicalType instanceof LogicalTypes.Decimal) {
            Object value = convert(DECIMAL_CONVERSION, fieldSchema, logicalType, getData().getField(datum, f.name(), f.pos()));
            writeWithoutConversion(fieldSchema, value, out);
        } else {
            super.writeField(datum, f, out, state);
        }
    }

    private Object convert(Conversion<?> conversion, Schema fieldSchema, LogicalType logicalType, Object value) {
        if (conversion instanceof Conversions.DecimalConversion && value instanceof ByteBuffer) {
            // convert decimal value to a string
            BigDecimal number = (BigDecimal) conversion.fromBytes((ByteBuffer) value, fieldSchema, logicalType);
            return (ByteBuffer.wrap(number.toPlainString().getBytes()));
        } else {
            return convert(fieldSchema, logicalType, conversion, value);
        }
    }
}
