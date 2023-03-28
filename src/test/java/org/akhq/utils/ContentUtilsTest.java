package org.akhq.utils;

import org.akhq.models.Record;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.Test;

import javax.swing.text.AbstractDocument;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContentUtilsTest {

    private static byte[] toBytes(Short value) {
        ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
        buffer.putShort(value);
        return buffer.array();
    }

    private static byte[] toBytes(Integer value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(value);
        return buffer.array();
    }

    private static byte[] toBytes(Long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    private static byte[] toBytes(Float value) {
        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        buffer.putFloat(value);
        return buffer.array();
    }

    private static byte[] toBytes(Double value) {
        ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES);
        buffer.putDouble(value);
        return buffer.array();
    }

    @Test
    void testHeaderValueStringUTF8() {
        String testValue = "Test";

        assertEquals(testValue, ContentUtils.convertToObject(testValue.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testHeaderValueInteger() {
        int testValue = 1;

        assertEquals(testValue, ContentUtils.convertToObject(toBytes(testValue)));
    }

    @Test
    void testHeaderValueLong() {
        long testValue = 111l;

        assertEquals(testValue, ContentUtils.convertToObject(toBytes(testValue)));
    }

    @Test
    void testHeaderValueShort() {
        short testValue = 10;

        assertEquals(testValue, ContentUtils.convertToObject(toBytes(testValue)));
    }

    @Test
    void testHeaderValueLongStringUTF8() {
        String testValue = RandomStringUtils.random(10000, true, false);

        assertEquals(testValue, ContentUtils.convertToObject(testValue.getBytes(StandardCharsets.UTF_8)));
    }

}
