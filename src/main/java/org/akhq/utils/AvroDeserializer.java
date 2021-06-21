package org.akhq.utils;

import org.apache.avro.Conversions.DecimalConversion;
import org.apache.avro.Conversions.UUIDConversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.data.TimeConversions.*;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericFixed;
import org.apache.avro.generic.GenericRecord;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class AvroDeserializer {
    private static final String DECIMAL = "decimal";
    private static final String UUID = "uuid";
    private static final String DATE = "date";
    private static final String TIME_MILLIS = "time-millis";
    private static final String TIME_MICROS = "time-micros";
    private static final String TIMESTAMP_MILLIS = "timestamp-millis";
    private static final String TIMESTAMP_MICROS = "timestamp-micros";
    
    private static final DecimalConversion DECIMAL_CONVERSION = new DecimalConversion();
    private static final UUIDConversion UUID_CONVERSION = new UUIDConversion();
    private static final DateConversion DATE_CONVERSION = new DateConversion();
    private static final TimeMicrosConversion TIME_MICROS_CONVERSION = new TimeMicrosConversion();
    private static final TimeMillisConversion TIME_MILLIS_CONVERSION = new TimeMillisConversion();
    private static final TimestampMicrosConversion TIMESTAMP_MICROS_CONVERSION = new TimestampMicrosConversion();
    private static final TimestampMillisConversion TIMESTAMP_MILLIS_CONVERSION = new TimestampMillisConversion();

    public static Map<String, Object> recordDeserializer(GenericRecord record) {
        return record
            .getSchema()
            .getFields()
            .stream()
            .collect(
                HashMap::new,
                (m, v) -> m.put(
                    v.name(),
                    AvroDeserializer.objectDeserializer(record.get(v.name()), v.schema())
                ),
                HashMap::putAll
            );
    }

    @SuppressWarnings("unchecked")
    private static Object objectDeserializer(Object value, Schema schema) {
        LogicalType logicalType = schema.getLogicalType();
        Type primitiveType = schema.getType();
        if (logicalType != null) {
            switch (logicalType.getName()) {
                case DATE:
                    return AvroDeserializer.dateDeserializer(value, schema, primitiveType, logicalType);
                case DECIMAL:
                    return AvroDeserializer.decimalDeserializer(value, schema, primitiveType, logicalType);
                case TIME_MICROS:
                    return AvroDeserializer.timeMicrosDeserializer(value, schema, primitiveType, logicalType);
                case TIME_MILLIS:
                    return AvroDeserializer.timeMillisDeserializer(value, schema, primitiveType, logicalType);
                case TIMESTAMP_MICROS:
                    return AvroDeserializer.timestampMicrosDeserializer(value, schema, primitiveType, logicalType);
                case TIMESTAMP_MILLIS:
                    return AvroDeserializer.timestampMillisDeserializer(value, schema, primitiveType, logicalType);
                case UUID:
                    return AvroDeserializer.uuidDeserializer(value, schema, primitiveType, logicalType);
                default:
                    throw new IllegalStateException("Unexpected value: " + logicalType);
            }
        } else {
            switch (primitiveType) {
                case UNION:
                    return AvroDeserializer.unionDeserializer(value, schema);
                case MAP:
                    return AvroDeserializer.mapDeserializer((Map<String, ?>) value, schema);
                case RECORD:
                    return AvroDeserializer.recordDeserializer((GenericRecord) value);
                case ENUM:
                    return value.toString();
                case ARRAY:
                    return arrayDeserializer((Collection<?>) value, schema);
                case FIXED:
                    return ((GenericFixed) value).bytes();
                case STRING:
                    return ((CharSequence) value).toString();
                case BYTES:
                    return ((ByteBuffer) value).array();
                case INT:
                case LONG:
                case FLOAT:
                case DOUBLE:
                case BOOLEAN:
                case NULL:
                    return value;
                default:
                    throw new IllegalStateException("Unexpected value: " + primitiveType);
            }
        }
    }

    private static Object unionDeserializer(Object value, Schema schema) {
        return AvroDeserializer.objectDeserializer(value, schema
            .getTypes()
            .stream()
            .filter(type -> {
                try {
                    return new GenericData().validate(type, value);
                } catch (Exception e) {
                    return  false;
                }
            })
            .findFirst()
            .orElseThrow());
    }

    private static Map<String, ?> mapDeserializer(Map<String, ?> value, Schema schema) {
        return value
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> AvroDeserializer.objectDeserializer(e.getValue(), schema.getValueType()))
            );
    }

    private static Collection<?> arrayDeserializer(Collection<?> value, Schema schema) {
        return value
            .stream()
            .map(e -> AvroDeserializer.objectDeserializer(e, schema.getElementType()))
            .collect(Collectors.toList());
    }

    private static Instant timestampMicrosDeserializer(Object value, Schema schema, Type primitiveType, LogicalType logicalType) {
        switch (primitiveType) {
            case LONG:
                return AvroDeserializer.TIMESTAMP_MICROS_CONVERSION.fromLong((Long) value, schema, logicalType);
            default:
                throw new IllegalStateException("Unexpected value: " + primitiveType);
        }
    }

    private static Instant timestampMillisDeserializer(Object value, Schema schema, Type primitiveType, LogicalType logicalType) {
        if (primitiveType == Type.LONG) {
            return AvroDeserializer.TIMESTAMP_MILLIS_CONVERSION.fromLong((Long) value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType);
    }

    private static LocalTime timeMicrosDeserializer(Object value, Schema schema, Type primitiveType, LogicalType logicalType) {
        if (primitiveType == Type.LONG) {
            return AvroDeserializer.TIME_MICROS_CONVERSION.fromLong((Long) value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType);
    }

    private static LocalTime timeMillisDeserializer(Object value, Schema schema, Type primitiveType, LogicalType logicalType) {
        if (primitiveType == Type.INT) {
            return AvroDeserializer.TIME_MILLIS_CONVERSION.fromInt((Integer) value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType);
    }

    private static LocalDate dateDeserializer(Object value, Schema schema, Type primitiveType, LogicalType logicalType) {
        if (primitiveType == Type.INT) {
            return AvroDeserializer.DATE_CONVERSION.fromInt((Integer) value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType);
    }

    private static UUID uuidDeserializer(Object value, Schema schema, Type primitiveType, LogicalType logicalType) {
        if (primitiveType == Type.STRING) {
            return AvroDeserializer.UUID_CONVERSION.fromCharSequence((CharSequence) value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType);
    }

    private static BigDecimal decimalDeserializer(Object value, Schema schema, Type primitiveType, LogicalType logicalType) {
        switch (primitiveType) {
            case BYTES:
                return AvroDeserializer.DECIMAL_CONVERSION.fromBytes((ByteBuffer) value, schema, logicalType);
            case FIXED:
                return AvroDeserializer.DECIMAL_CONVERSION.fromFixed((GenericFixed) value, schema, logicalType);
            default:
                throw new IllegalStateException("Unexpected value: " + primitiveType);
        }
    }
}

