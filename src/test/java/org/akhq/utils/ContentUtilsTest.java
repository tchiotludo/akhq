package org.akhq.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    @Test
    void testHeaderValueMultiBytesUTF8() {
        String testValue = "日本語テキスト😀with emoji";

        assertEquals(testValue, ContentUtils.convertToObject(testValue.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testInvalidUtf8BytesAreNotDecodedAsString() {
        // 0x80 is a continuation byte on its own, so this is not valid UTF-8: it must go down the
        // numeric path instead of being shown as a (mojibake) string.
        byte[] value = new byte[8];
        Arrays.fill(value, (byte) 0x80);

        assertEquals(Long.class, ContentUtils.convertToObject(value).getClass());
    }

    @Test
    void testInvalidUtf8BytesUpToTheRegexLimitAreNotDecodedAsString() {
        byte[] value = new byte[1000];
        Arrays.fill(value, (byte) 0x80);

        assertEquals(Long.class, ContentUtils.convertToObject(value).getClass());
    }

    @Test
    void testValueAboveTheRegexLimitIsAlwaysDecodedAsString() {
        // Above 1000 bytes the content is assumed to be a String, since matching the UTF-8 pattern
        // on a long value used to throw a StackOverflowError.
        byte[] value = new byte[1001];
        Arrays.fill(value, (byte) 0x80);

        assertEquals(new String(value), ContentUtils.convertToObject(value));
    }

}
