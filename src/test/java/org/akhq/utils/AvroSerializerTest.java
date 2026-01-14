package org.akhq.utils;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvroSerializerTest {

    @Nested
    class ParseDateTime {

        @Nested
        class Utc {

            @Test
            void testParseDateTime_micros_utc() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12.345678Z"),
                    Instant.parse("2021-07-16T21:30:12.345678Z"));
            }

            @Test
            void testParseDateTime_millis_utc() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12.345Z"),
                    Instant.parse("2021-07-16T21:30:12.345Z"));
            }

            @Test
            void testParseDateTime_seconds_utc() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12Z"),
                    Instant.parse("2021-07-16T21:30:12Z"));
            }

            @Test
            void testParseDateTime_minutes_utc() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30Z"),
                    Instant.parse("2021-07-16T21:30:00Z"));
            }

        }

        @Nested
        class Offset {

            @Test
            void testParseDateTime_micros_offset() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12.345678+08:00"),
                    Instant.parse("2021-07-16T13:30:12.345678Z"));
            }

            @Test
            void testParseDateTime_micros_offset_short() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12.345678+08"),
                    Instant.parse("2021-07-16T13:30:12.345678Z"));
            }

            @Test
            void testParseDateTime_millis_offset() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12.345+08:00"),
                    Instant.parse("2021-07-16T13:30:12.345Z"));
            }

            @Test
            void testParseDateTime_millis_offset_short() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12.345+08"),
                    Instant.parse("2021-07-16T13:30:12.345Z"));
            }

            @Test
            void testParseDateTime_seconds_offset() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12+08:00"),
                    Instant.parse("2021-07-16T13:30:12Z"));
            }

            @Test
            void testParseDateTime_seconds_offset_short() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12+08"),
                    Instant.parse("2021-07-16T13:30:12Z"));
            }

            @Test
            void testParseDateTime_minutes_offset() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30+08:00"),
                    Instant.parse("2021-07-16T13:30:00Z"));
            }

            @Test
            void testParseDateTime_minutes_offset_short() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30+08"),
                    Instant.parse("2021-07-16T13:30:00Z"));
            }

        }

        @Nested
        class Local {

            @Test
            void testParseDateTime_micros_local() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12.345678"),
                    LocalDateTime.parse("2021-07-16T21:30:12.345678").atZone(ZoneId.systemDefault()).toInstant());
            }

            @Test
            void testParseDateTime_millis_local() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12.345"),
                    LocalDateTime.parse("2021-07-16T21:30:12.345").atZone(ZoneId.systemDefault()).toInstant());
            }

            @Test
            void testParseDateTime_seconds_local() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30:12"),
                    LocalDateTime.parse("2021-07-16T21:30:12").atZone(ZoneId.systemDefault()).toInstant());
            }

            @Test
            void testParseDateTime_minutes_local() {
                assertEquals(AvroSerializer.parseDateTime("2021-07-16T21:30"),
                    LocalDateTime.parse("2021-07-16T21:30").atZone(ZoneId.systemDefault()).toInstant());
            }

        }

    }

    private final org.apache.avro.Schema SCHEMA = SchemaBuilder
        .record("schema1").namespace("org.akhq")
        .fields()
        .name("title").type().stringType().noDefault()
        .name("release_year").type().intType().noDefault()
        .name("rating").type().doubleType().noDefault()
        .endRecord();

    @Test
    void shouldThrowIfSchemaAndRecordFieldsAreNotEqual() {
        assertThrows(IllegalArgumentException.class, () -> {
            AvroSerializer.recordSerializer(Map.of("title", "akhq"), SCHEMA);
        });
    }

    @Nested
    static class AmbiguousUnion {

        @Test
        void shouldTakeCorrectUnionTypeIfSpecified() {
            verifyUnionWithSpecifiedType(UNION_TYPE_1);
            verifyUnionWithSpecifiedType(UNION_TYPE_2);
        }

        @Test
        void shouldTakeFirstUnionTypeIfNotSpecified() {
            Map<String, Object> unionField = new HashMap<>();
            unionField.put("label", "label");

            Map<String, Object> rootAvro = new HashMap<>();
            rootAvro.put("unionField", unionField);

            verifyUnionTypeAfterSerialization(rootAvro, UNION_TYPE_1);
        }

        private static void verifyUnionWithSpecifiedType(String specifiedType) {
            Map<String, Object> unionField = new HashMap<>();
            unionField.put("label", "label");

            Map<String, Object> unionFieldWithSpecifiedType = new HashMap<>();
            unionFieldWithSpecifiedType.put(specifiedType, unionField);

            Map<String, Object> rootAvro = new HashMap<>();
            rootAvro.put("unionField", unionFieldWithSpecifiedType);

            verifyUnionTypeAfterSerialization(rootAvro, specifiedType);
        }

        private static void verifyUnionTypeAfterSerialization(Map<String, Object> rootAvro, String expectedType) {
            GenericRecord serializedRecord = AvroSerializer.recordSerializer(rootAvro, AmbiguousUnion.AMBIGUOUS_UNION_SCHEMA);
            assertThat(((GenericData.Record) serializedRecord.get(0)).getSchema().getName(), is(expectedType));
        }

        private static final String UNION_TYPE_1 = "UnionType1";
        private static final String UNION_TYPE_2 = "UnionType2";

        private static final String AMBIGUOUS_UNION_SCHEMA_STRING = "{"
            + "\"name\": \"root\","
            + "\"type\": \"record\","
            + "\"fields\": ["
            + "    {\"name\": \"unionField\",\"type\": [{\"type\": \"record\", \"name\": \"UnionType1\", \"fields\": [{\"name\": \"label\", \"type\": \"string\"}]},{\"type\": \"record\", \"name\": \"UnionType2\", \"fields\": [{\"name\": \"label\", \"type\": \"string\"}]}]}"
            + "    ]"
            + "}";

        private static final Schema AMBIGUOUS_UNION_SCHEMA = new Schema.Parser().parse(AMBIGUOUS_UNION_SCHEMA_STRING);
    }
}
