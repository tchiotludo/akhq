package org.akhq.utils;

import org.apache.avro.Conversions;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.data.TimeConversions;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Collectors;

public class AvroSerializer {
    private static final String DECIMAL = "decimal";
    private static final String UUID = "uuid";
    private static final String DATE = "date";
    private static final String TIME_MILLIS = "time-millis";
    private static final String TIME_MICROS = "time-micros";
    private static final String TIMESTAMP_MILLIS = "timestamp-millis";
    private static final String TIMESTAMP_MICROS = "timestamp-micros";
    private static final String LOCAL_TIMESTAMP_MILLIS = "local-timestamp-millis";
    private static final String LOCAL_TIMESTAMP_MICROS = "local-timestamp-micros";

    private static final Conversions.DecimalConversion DECIMAL_CONVERSION = new Conversions.DecimalConversion();
    private static final Conversions.UUIDConversion UUID_CONVERSION = new Conversions.UUIDConversion();
    private static final TimeConversions.DateConversion DATE_CONVERSION = new TimeConversions.DateConversion();
    private static final TimeConversions.TimeMicrosConversion TIME_MICROS_CONVERSION = new TimeConversions.TimeMicrosConversion();
    private static final TimeConversions.TimeMillisConversion TIME_MILLIS_CONVERSION = new TimeConversions.TimeMillisConversion();
    private static final TimeConversions.TimestampMicrosConversion TIMESTAMP_MICROS_CONVERSION = new TimeConversions.TimestampMicrosConversion();
    private static final TimeConversions.TimestampMillisConversion TIMESTAMP_MILLIS_CONVERSION = new TimeConversions.TimestampMillisConversion();
    private static final TimeConversions.LocalTimestampMicrosConversion LOCAL_TIMESTAMP_MICROS_CONVERSION = new TimeConversions.LocalTimestampMicrosConversion();
    private static final TimeConversions.LocalTimestampMillisConversion LOCAL_TIMESTAMP_MILLIS_CONVERSION = new TimeConversions.LocalTimestampMillisConversion();

    protected static final String DATE_FORMAT = "yyyy-MM-dd[XXX]";
    protected static final String TIME_FORMAT = "HH:mm[:ss][.SSSSSS][XXX]";
    protected static final DateTimeFormatter DATETIME_FORMAT = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .optionalStart()
            .parseLenient()
            .appendOffsetId()
            .toFormatter();

    public static GenericRecord recordSerializer(Map<String, Object> record, Schema schema) {
        validateSchema(schema.getFields(), record);

        GenericRecord returnValue = new GenericData.Record(schema);
        schema
            .getFields()
            .forEach(field -> {
                Object fieldValue = record.getOrDefault(field.name(), field.defaultVal());
                returnValue.put(field.name(), AvroSerializer.objectSerializer(fieldValue, field.schema()));
            });

        return returnValue;
    }

    private static void validateSchema(List<Schema.Field> fields, Map<String, Object> record) {
        for (Schema.Field field : fields) {
            var schema = field.schema();
            var type = schema.getType();
            var value = Optional.ofNullable(record)
                .filter(Objects::nonNull)
                .map(r -> r.get(field.name()));
            var hasEmptyValue = value.isEmpty();

            validateSchemaHasDefaultValue(field, schema, hasEmptyValue);

            if (Schema.Type.RECORD.getName().equals(type.getName()) && !hasEmptyValue) {
                validateSchema(schema.getFields(), (Map<String, Object>) value.get());
            }
            else if (Schema.Type.ARRAY.getName().equals(type.getName()) && !hasEmptyValue) {
                Schema elementType = schema.getElementType();
                if (elementType.getType().equals(Schema.Type.RECORD)) {
                    for(Map<String, Object> val : (List<Map<String, Object>>) value.get()) {
                        validateSchema(elementType.getFields(), val);
                    }
                }
            }
        }
    }

    private static void validateSchemaHasDefaultValue(Schema.Field field, Schema schema, boolean hasEmptyValue) {
        var isFieldHasNullValue = field.hasDefaultValue() || schema.isNullable();

        if ((!isFieldHasNullValue) && hasEmptyValue) {
            var message = String.format("Field %s is missing in the payload", field.name());
            throw new IllegalArgumentException(message);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object objectSerializer(Object value, Schema schema) {
        if (value == org.apache.avro.JsonProperties.NULL_VALUE) {
            return null;
        }
        LogicalType logicalType = schema.getLogicalType();
        Schema.Type primitiveType = schema.getType();
        if (logicalType != null) {
            switch (logicalType.getName()) {
                case DATE:
                    return AvroSerializer.dateSerializer(value, schema, primitiveType, logicalType);
                case DECIMAL:
                    return AvroSerializer.decimalSerializer(value, schema, primitiveType, logicalType);
                case TIME_MICROS:
                    return AvroSerializer.timeMicrosSerializer(value, schema, primitiveType, logicalType);
                case TIME_MILLIS:
                    return AvroSerializer.timeMillisSerializer(value, schema, primitiveType, logicalType);
                case TIMESTAMP_MICROS:
                    return AvroSerializer.timestampMicrosSerializer(value, schema, primitiveType, logicalType);
                case TIMESTAMP_MILLIS:
                    return AvroSerializer.timestampMillisSerializer(value, schema, primitiveType, logicalType);
                case LOCAL_TIMESTAMP_MICROS:
                    return AvroSerializer.localTimestampMicrosSerializer(value, schema, primitiveType, logicalType);
                case LOCAL_TIMESTAMP_MILLIS:
                    return AvroSerializer.localTimestampMillisSerializer(value, schema, primitiveType, logicalType);
                case UUID:
                    return AvroSerializer.uuidSerializer(value, schema, primitiveType, logicalType);
                default:
                    throw new IllegalStateException("Unexpected value: " + logicalType);
            }
        } else {
            switch (primitiveType) {
                case UNION:
                    return AvroSerializer.unionSerializer(value, schema);
                case MAP:
                    return AvroSerializer.mapSerializer((Map<String, ?>) value, schema);
                case RECORD:
                    return AvroSerializer.recordSerializer((Map<String, Object>) value, schema);
                case ENUM:
                    return new GenericData.EnumSymbol(schema, value.toString());
                case ARRAY:
                    return arraySerializer((Collection<?>) value, schema);
                case FIXED:
                    return new GenericData.Fixed(schema, (byte[]) value);
                case STRING:
                    return new Utf8((String) value);
                case BYTES:
                    if (value instanceof byte[]) {
                        return ByteBuffer.wrap((byte[]) value);
                    } else {
                        return ByteBuffer.wrap(((String) value).getBytes());
                    }
                case INT:
                    return value;
                case LONG:
                   if (value instanceof Integer) {
                        return ((Integer) value).longValue();
                    }
                    return value;
                case FLOAT:
                    if (value instanceof Integer) {
                        return ((Integer) value).floatValue();
                    } else if (value instanceof Long) {
                        return ((Long) value).floatValue();
                    }
                    return value;
                case DOUBLE:
                    if (value instanceof Integer) {
                        return ((Integer) value).doubleValue();
                    } else if (value instanceof Long) {
                        return ((Long) value).doubleValue();
                    }
                    return value;
                case BOOLEAN:
                    return value;
                case NULL:
                    if (value == null) {
                        return null;
                    }
                default:
                    throw new IllegalStateException("Unexpected value: " + primitiveType);
            }
        }
    }

    private static Object unionSerializer(Object value, Schema schema) {
        return AvroSerializer.objectSerializer(value, schema
            .getTypes()
            .stream()
            .filter(type -> {
                try {
                    objectSerializer(value, type);
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            }).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unable to find on '" + value + "' schema '" + schema + "'")));
    }

    private static Map<String, ?> mapSerializer(Map<String, ?> value, Schema schema) {
        return value
            .entrySet()
            .stream()
            .map(r -> new AbstractMap.SimpleEntry<>(
                r.getKey(),
                AvroSerializer.objectSerializer(r.getValue(), schema.getValueType())
            ))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Collection<?> arraySerializer(Collection<?> value, Schema schema) {
        return value
            .stream()
            .map(e -> AvroSerializer.objectSerializer(e, schema.getElementType()))
            .collect(Collectors.toList());
    }

    private static Long timestampMicrosSerializer(Object data, Schema schema, Schema.Type primitiveType, LogicalType logicalType) {
        Instant value;

        if (data instanceof String) {
            try {
                value = Instant.ofEpochSecond(0, Long.parseLong((String) data) * 1000);
            } catch (NumberFormatException ignored) {
                value = AvroSerializer.parseDateTime((String) data);
            }
        } else if (data instanceof Long) {
            value = Instant.ofEpochSecond(0, (Long) data * 1000);
        } else if (data instanceof Integer) {
            value = Instant.ofEpochSecond(0, ((Integer) data).longValue() * 1000);
        } else {
            value = (Instant) data;
        }

        if (primitiveType == Schema.Type.LONG) {
            return AvroSerializer.TIMESTAMP_MICROS_CONVERSION.toLong(value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType + " on schema " + schema);
    }

    private static Long timestampMillisSerializer(Object data, Schema schema, Schema.Type primitiveType, LogicalType logicalType) {
        Instant value;

        if (data instanceof String) {
            try {
                value = Instant.ofEpochMilli(Long.parseLong((String) data));
            } catch (NumberFormatException ignored) {
                value = AvroSerializer.parseDateTime((String) data);
            }
        } else if (data instanceof Long) {
            value = Instant.ofEpochMilli((Long) data);
        } else if (data instanceof Integer) {
            value = Instant.ofEpochMilli(((Integer) data).longValue());
        } else {
            value = (Instant) data;
        }

        if (primitiveType == Schema.Type.LONG) {
            return AvroSerializer.TIMESTAMP_MILLIS_CONVERSION.toLong(value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType + " on schema " + schema);
    }

    private static Long localTimestampMicrosSerializer(Object data, Schema schema, Schema.Type primitiveType, LogicalType logicalType) {
        LocalDateTime value;

        if (data instanceof String) {
            try {
                value = LocalDateTime.ofInstant(Instant.ofEpochSecond(0, Long.parseLong((String) data) * 1000), ZoneOffset.UTC);
            } catch (NumberFormatException ignored) {
                value = LocalDateTime.parse((String) data);
            }
        } else if (data instanceof Long) {
            value = LocalDateTime.ofInstant(Instant.ofEpochSecond(0, (Long) data * 1000), ZoneOffset.UTC);
        } else if (data instanceof Integer) {
            value = LocalDateTime.ofInstant(Instant.ofEpochSecond(0, ((Integer) data).longValue() * 1000), ZoneOffset.UTC);
        } else {
            value = (LocalDateTime) data;
        }

        if (primitiveType == Schema.Type.LONG) {
            return AvroSerializer.LOCAL_TIMESTAMP_MICROS_CONVERSION.toLong(value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType + " on schema " + schema);
    }

    private static Long localTimestampMillisSerializer(Object data, Schema schema, Schema.Type primitiveType, LogicalType logicalType) {
        LocalDateTime value;

        if (data instanceof String) {
            try {
                value = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong((String) data)), ZoneOffset.UTC);
            } catch (NumberFormatException ignored) {
                value = LocalDateTime.parse((String) data);
            }
        } else if (data instanceof Long) {
            value = LocalDateTime.ofInstant(Instant.ofEpochMilli((Long) data), ZoneOffset.UTC);
        } else if (data instanceof Integer) {
            value = LocalDateTime.ofInstant(Instant.ofEpochMilli(((Integer) data).longValue()), ZoneOffset.UTC);
        } else {
            value = (LocalDateTime) data;
        }

        if (primitiveType == Schema.Type.LONG) {
            return AvroSerializer.LOCAL_TIMESTAMP_MILLIS_CONVERSION.toLong(value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType + " on schema " + schema);
    }

    protected static Instant parseDateTime(String data) {
        TimeZone tz = TimeZone.getDefault();
        return DATETIME_FORMAT.withZone(tz.toZoneId()).parse(data, Instant::from);
    }

    private static Long timeMicrosSerializer(Object data, Schema schema, Schema.Type primitiveType, LogicalType logicalType) {
        LocalTime value;
        if (data instanceof String) {
            value = LocalTime.parse((String) data, DateTimeFormatter.ofPattern(AvroSerializer.TIME_FORMAT));
        } else {
            value = (LocalTime) data;
        }

        if (primitiveType == Schema.Type.LONG) {
            return AvroSerializer.TIME_MICROS_CONVERSION.toLong(value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType + " on schema " + schema);
    }

    private static Integer timeMillisSerializer(Object data, Schema schema, Schema.Type primitiveType, LogicalType logicalType) {
        LocalTime value;

        if (data instanceof String) {
            value = LocalTime.parse((String) data, DateTimeFormatter.ofPattern(AvroSerializer.TIME_FORMAT));
        } else {
            value = (LocalTime) data;
        }

        if (primitiveType == Schema.Type.INT) {
            return AvroSerializer.TIME_MILLIS_CONVERSION.toInt(value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType + " on schema " + schema);
    }

    private static Integer dateSerializer(Object data, Schema schema, Schema.Type primitiveType, LogicalType logicalType) {
        LocalDate value;
        if (data instanceof String) {
            value = LocalDate.parse((String) data, DateTimeFormatter.ofPattern(AvroSerializer.DATE_FORMAT));
        } else {
            value = (LocalDate) data;
        }

        if (primitiveType == Schema.Type.INT) {
            return AvroSerializer.DATE_CONVERSION.toInt(value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType + " on schema " + schema);
    }

    private static CharSequence uuidSerializer(Object data, Schema schema, Schema.Type primitiveType, LogicalType logicalType) {
        java.util.UUID value;

        if (data instanceof String) {
            value = java.util.UUID.fromString((String) data);
        } else {
            value = (UUID) data;
        }

        if (primitiveType == Schema.Type.STRING) {
            return AvroSerializer.UUID_CONVERSION.toCharSequence(value, schema, logicalType);
        }

        throw new IllegalStateException("Unexpected value: " + primitiveType + " on schema " + schema);
    }

    @SuppressWarnings("UnpredictableBigDecimalConstructorCall")
    private static Object decimalSerializer(Object data, Schema schema, Schema.Type primitiveType, LogicalType logicalType) {
        int scale = ((LogicalTypes.Decimal) schema.getLogicalType()).getScale();
        int precision = ((LogicalTypes.Decimal) schema.getLogicalType()).getPrecision();
        double multiply = Math.pow(10D, precision - scale * 1D);

        BigDecimal value;

        if (data instanceof String) {
            value = new BigDecimal((String) data);
        } else if (data instanceof Long) {
            value = BigDecimal.valueOf((long) ((long) data * multiply), scale);
        } else if (data instanceof Integer) {
            value = BigDecimal.valueOf((int) ((int) data * multiply), scale);
        } else if (data instanceof Double) {
            value = new BigDecimal((double) data, new MathContext(precision));
        } else if (data instanceof Float) {
            value = new BigDecimal((float) data, new MathContext(precision));
        } else {
            value = (BigDecimal) data;
        }

        value = value.setScale(scale, RoundingMode.HALF_EVEN);

        switch (primitiveType) {
            case BYTES:
                return AvroSerializer.DECIMAL_CONVERSION.toBytes(value, schema, logicalType);
            case FIXED:
                return AvroSerializer.DECIMAL_CONVERSION.toFixed(value, schema, logicalType);
            default:
                throw new IllegalStateException("Unexpected value: " + primitiveType);
        }
    }
}

