package org.akhq.utils.avroserdes;

import org.apache.avro.Conversion;
import org.apache.avro.Conversions;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;

class BigDecimalFriendlyGenericDatumWriter<T> extends GenericDatumWriter<T> {

    private static final Conversion<BigDecimal> DECIMAL_CONVERSION = new Conversions.DecimalConversion();

    public BigDecimalFriendlyGenericDatumWriter(Schema schema) {
        super(schema);
    }

    @Override
    protected void writeField(Object datum, Schema.Field f, Encoder out, Object state) throws IOException {
        Schema fieldSchema = f.schema();
        LogicalType logicalType = fieldSchema.getLogicalType();

        if (datum instanceof GenericData.Record && logicalType instanceof LogicalTypes.Decimal) {
            BigDecimal number = (BigDecimal) DECIMAL_CONVERSION.fromBytes(
                    (ByteBuffer) getData().getField(datum, f.name(), f.pos()), fieldSchema, logicalType);
            Object value = ByteBuffer.wrap(number.toPlainString().getBytes());
            writeWithoutConversion(fieldSchema, value, out);
        } else {
            super.writeField(datum, f, out, state);
        }
    }
}
