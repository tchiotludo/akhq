package org.akhq.utils;

import org.apache.avro.SchemaBuilder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvroSerializerTest {

    @Nested
    static class ParseDateTime {

        @Nested
        static class Utc {

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
        static class Offset {

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
        static class Local {

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

}
