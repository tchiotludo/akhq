package org.akhq.utils.avroserde;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;

import org.apache.avro.Conversion;
import org.apache.avro.Conversions;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.ResolvingDecoder;
import org.apache.avro.specific.SpecificDatumReader;

/**
 * DatumReader which reads BigDecimals from a readable String.
 */
class BigDecimalFriendlySpecificDatumReader<T> extends SpecificDatumReader<T> {
    private static final Conversion<BigDecimal> DECIMAL_CONVERSION = new Conversions.DecimalConversion();

    public BigDecimalFriendlySpecificDatumReader(Schema schema) {
        super(schema);
    }

    @Override
    protected void readField(Object record, Schema.Field f, Object oldDatum, ResolvingDecoder resolver, Object state) throws IOException {
        Schema schema = f.schema();
        LogicalType logicalType = schema.getLogicalType();

        if (record instanceof GenericData.Record &&
                schema.getType() == Schema.Type.BYTES &&
                logicalType instanceof LogicalTypes.Decimal) {
            String value = ((ByteBuffer) readBytes(oldDatum, schema, resolver)).toString();
            Object datum = DECIMAL_CONVERSION.toBytes(new BigDecimal(value), schema, logicalType);
            getData().setField(record, f.name(), f.pos(), datum);
        } else {
            super.readField(record, f, oldDatum, resolver, state);
        }
    }
}
