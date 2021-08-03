package org.akhq.utils;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class AvroDeserializerTest {
    private static Schema fieldsToSchema(String s) {
        return new Schema.Parser().parse("{\"name\": \"root\", \"type\":\"record\", \"fields\": [" + s + "]}");
    }

    static Stream<Arguments> primitiveSource() {
        return Stream.of(
            Arguments.of(null, "\"null\""),
            Arguments.of(true, "\"boolean\""),
            Arguments.of(1, "\"int\""),
            Arguments.of(10000000000000L, "\"long\""),
            Arguments.of(1.0F, "\"float\""),
            Arguments.of(2.0D, "\"double\""),
            Arguments.of("abc", "\"string\""),
            Arguments.of("abc".getBytes(), "\"bytes\""),
            Arguments.of(new BigDecimal("10.10"), "{\"type\": \"bytes\", \"logicalType\": \"decimal\", \"scale\": 2, \"precision\": 4}"),
            Arguments.of(new BigDecimal("26910000000000000000000000000258.00000"), "{\"type\": \"bytes\", \"logicalType\": \"decimal\", \"scale\": 5, \"precision\": 37}"),
            Arguments.of(UUID.randomUUID(), "{\"type\": \"string\", \"logicalType\": \"uuid\"}"),
            Arguments.of(LocalDate.now(), "{\"type\": \"int\", \"logicalType\": \"date\"}"),
            Arguments.of(LocalTime.now(Clock.tickMillis(ZoneId.of("UTC"))), "{\"type\": \"int\", \"logicalType\": \"time-millis\"}"),
            Arguments.of(LocalTime.now(), "{\"type\": \"long\", \"logicalType\": \"time-micros\"}"),
            Arguments.of(Instant.now(Clock.tickMillis(ZoneId.of("UTC"))), "{\"type\": \"long\", \"logicalType\": \"timestamp-millis\"}"),
            Arguments.of(Instant.now(), "{\"type\": \"long\", \"logicalType\": \"timestamp-micros\"}"),
            Arguments.of(Instant.now(), "[\"null\", {\"type\": \"long\", \"logicalType\": \"timestamp-micros\"}]")
        );
    }

    static void genericTest(Object expected, String type) {
        String fieldName = "test";
        Schema schema = fieldsToSchema("{\"name\": \"" + fieldName + "\", \"type\": " + type + "}");

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put(fieldName, expected);

        GenericRecord expectedRecord = AvroSerializer.recordSerializer(expectedMap, schema);
        Map<String, Object> result = AvroDeserializer.recordDeserializer(expectedRecord);

        assert new GenericData().validate(schema, expectedRecord);

        assertThat(result, is(expectedMap));
    }

    @ParameterizedTest
    @MethodSource("primitiveSource")
    void testPrimitive(Object value, String type) {
        genericTest(value, type);
    }

    static Stream<Arguments> convertionSource() {
        UUID uuid = UUID.randomUUID();
        LocalTime localTime = LocalTime.now();
        Instant now = Instant.now();

        return Stream.of(
            Arguments.of("abc", "\"bytes\"", "abc".getBytes()),
            Arguments.of("10.10", "{\"type\": \"bytes\", \"logicalType\": \"decimal\", \"scale\": 2, \"precision\": 4}", new BigDecimal("10.10")),
            Arguments.of("26910000000000000000000000000258.00000", "{\"type\": \"bytes\", \"logicalType\": \"decimal\", \"scale\": 5, \"precision\": 37}", new BigDecimal("26910000000000000000000000000258.00000")),
            Arguments.of(1, "[\"null\",\"long\"]", 1L),
            Arguments.of(uuid.toString(), "{\"type\": \"string\", \"logicalType\": \"uuid\"}", uuid),
            Arguments.of(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), "{\"type\": \"int\", \"logicalType\": \"date\"}", LocalDate.now()),
            Arguments.of(localTime.format(DateTimeFormatter.ISO_LOCAL_TIME), "{\"type\": \"long\", \"logicalType\": \"time-micros\"}", localTime),
            Arguments.of(now.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), "{\"type\": \"long\", \"logicalType\": \"timestamp-micros\"}", now)
        );
    }

    @ParameterizedTest
    @MethodSource("convertionSource")
    void testConvertion(Object value, String type, Object converted) {
        String fieldName = "test";
        Schema schema = fieldsToSchema("{\"name\": \"" + fieldName + "\", \"type\": " + type + "}");

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put(fieldName, value);

        GenericRecord expectedRecord = AvroSerializer.recordSerializer(expectedMap, schema);
        Map<String, Object> result = AvroDeserializer.recordDeserializer(expectedRecord);

        assertThat(result.get(fieldName), is(converted));
    }

    @Test
    void testNestedRecord() {
        String type = "{\"name\":\"test\", \"type\": \"record\", \"fields\": [{\"name\": \"foo\", \"type\": {\"name\":\"foo\", \"type\":\"record\", \"fields\": [{\"name\": \"bar\", \"type\": \"int\"}]}}]}";
        Map<String, Object> expected = new HashMap<>();
        Map<String, Object> foo = new HashMap<>();
        expected.put("foo", foo);
        foo.put("bar", 1);

        genericTest(expected, type);
    }

    @Test
    void testCompleteObject() {
        String type = "{"
            + "\"name\": \"X\","
            + "\"type\": \"record\","
            + "\"fields\": ["
            + "    {\"name\": \"intField\", \"type\": \"int\"},"
            + "    {\"name\": \"longField\", \"type\": \"long\"},"
            + "    {\"name\": \"stringField\", \"type\": \"string\"},"
            + "    {\"name\": \"boolField\", \"type\": \"boolean\"},"
            + "    {\"name\": \"floatField\", \"type\": \"float\"},"
            + "    {\"name\": \"doubleField\", \"type\": \"double\"},"
            + "    {\"name\": \"bytesField\", \"type\": \"bytes\"},"
            + "    {\"name\": \"nullField\", \"type\": \"null\"},"
            + "    {\"name\": \"unionField\",\"type\": [\"boolean\", \"double\", {\"type\": \"array\", \"items\": \"bytes\"}]},"
            + "    {"
            + "        \"name\": \"enumField\","
            + "        \"type\": {\"type\": \"enum\", \"name\": \"Kind\", \"symbols\": [\"A\", \"B\", \"C\"]}"
            + "    },"
            + "    {"
            + "        \"name\": \"fixedField\","
            + "        \"type\": {\"type\": \"fixed\", \"name\": \"MD5\", \"size\": 16}"
            + "    },"
            + "    {\"name\": \"recordField\", \"type\": {\"type\": \"record\", \"name\": \"Node\", \"fields\": [{\"name\": \"label\", \"type\": \"string\"},{\"name\": \"children\", \"type\": {\"type\": \"array\", \"items\": \"int\"}}]}},"
            + "    {\"name\": \"arrayField\", \"type\": {\"type\": \"array\", \"items\": \"double\"}}"
            + "    ]"
            + "}";

        Map<String, Object> expected = new HashMap<>();
        expected.put("intField", 1);
        expected.put("longField", 10000000000L);
        expected.put("stringField", "I was here");
        expected.put("boolField", true);
        expected.put("floatField", 1.0F);
        expected.put("doubleField", 1.0D);
        expected.put("bytesField", "akhq rulez".getBytes());
        expected.put("nullField", null);
        expected.put("unionField", 2.0D);
        expected.put("enumField", "A");
        expected.put("fixedField", "1234567890ABCDEF".getBytes());
        Map<String, Object> recordField = new HashMap<>();
        expected.put("recordField", recordField);
        recordField.put("label", "label");
        recordField.put("children", List.of(1, 2, 3));
        expected.put("arrayField", List.of(1.0D, 2.0D, 3.0D));

        genericTest(expected, type);
    }

    @Test
    void testDefaultValue() {
        String type = "{"
            + "\"name\": \"root\","
            + "\"type\": \"record\","
            + "\"fields\": ["
            + "    {\"name\": \"stringField\", \"type\": [\"null\", \"string\"], \"default\": null},"
            + "    {\"name\": \"arrayField\", \"type\": {\"type\": \"array\", \"items\": \"double\"}, \"default\": []}"
            + "    ]"
            + "}";
        Schema schema = new Schema.Parser().parse(type);

        GenericRecord expectedRecord = AvroSerializer.recordSerializer(Map.of(), schema);
        assert new GenericData().validate(schema, expectedRecord);

        Map<String, Object> result = AvroDeserializer.recordDeserializer(expectedRecord);
        Map<String, Object> defaultValues = new HashMap<>();
        defaultValues.put("stringField", null);
        defaultValues.put("arrayField", List.of());
        assertThat(result, is(defaultValues));
    }
}
