package org.akhq.utils;

import com.google.gson.JsonParser;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(environments = "json-mask-by-default-data-masking")
class JsonMaskByDefaultMaskerTest extends MaskerTestHelper {

    @Inject
    Masker masker;

    @Test
    void shouldUseJsonMaskByDefaultMasker() {
        assertInstanceOf(JsonMaskByDefaultMasker.class, masker);
    }

    @Test
    void shouldMaskRecordValue() {
        Record record = sampleRecord(
            "users",
            "some-key",
            sampleValue()
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            "{\"specialId\":123,\"status\":\"ACTIVE\",\"name\":\"xxxx\",\"dateOfBirth\":\"xxxx\",\"address\":{\"firstLine\":\"xxxx\",\"town\":\"xxxx\",\"country\":\"United Kingdom\"},\"metadata\":{\"trusted\":true,\"rating\":\"10\",\"notes\":\"xxxx\"}}",
            maskedRecord.getValue()
        );
    }

    @Test
    void forUndefinedTopicShouldDefaultMaskAllValues() {
        Record record = sampleRecord(
            "different-topic",
            "some-key",
            sampleValue()
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            "{\"specialId\":\"xxxx\",\"status\":\"xxxx\",\"name\":\"xxxx\",\"dateOfBirth\":\"xxxx\",\"address\":{\"firstLine\":\"xxxx\",\"town\":\"xxxx\",\"country\":\"xxxx\"},\"metadata\":{\"trusted\":\"xxxx\",\"rating\":\"xxxx\",\"notes\":\"xxxx\"}}",
            maskedRecord.getValue()
        );
    }

    @Test
    void forTombstoneShouldReturnItself() {
        Record record = sampleRecord(
            "users",
            "some-key",
            null
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            record,
            maskedRecord
        );
    }

    @Test
    void forNonJsonValueShouldReturnItself() {
        Record record = sampleRecord(
            "users",
            "some-key",
            "not a valid json"
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            "This record is unable to be masked as it is not a structured object. " +
                "This record is unavailable to view due to safety measures from json_mask_by_default to not leak " +
                "sensitive data. Please contact akhq administrator.",
            maskedRecord.getValue()
        );
    }

    @Test
    void forNonJsonValueThatLooksLikeJsonValueShouldReturnDisclaimer() {
        Record record = sampleRecord(
            "users",
            "some-key",
            "{not a valid json}"
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            "This record is unable to be masked as it is not a structured object. " +
                "This record is unavailable to view due to safety measures from json_mask_by_default to not leak " +
                "sensitive data. Please contact akhq administrator.",
            maskedRecord.getValue()
        );
    }

    @Test
    void ifJsonParsingThrowsExceptionShouldReturnFalse() {
        String sampleStringToParse = sampleValue();
        Record record = sampleRecord(
            "different-topic",
            "some-key",
            sampleStringToParse
        );

        try (MockedStatic<JsonParser> mockStatic = Mockito.mockStatic(JsonParser.class)) {
            mockStatic.when(() -> JsonParser.parseString(sampleStringToParse)).thenThrow(new RuntimeException("Bad exception!"));
            Record record1 = masker.maskRecord(record);
            assertEquals("An exception occurred during an attempt to mask this record. This record is unavailable to view due to safety measures from json_mask_by_default to not leak sensitive data. Please contact akhq administrator.", record1.getValue());
        }
    }

    @Test
    void ifRecordHasMultiLevelNestedValuesShouldBeProcessedCorrectly() {
        Record record = sampleRecord(
            "users",
            "some-key",
            sampleValueWithMultilevelNestedValues()
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            """
                {"specialId":123,"status":"ACTIVE","name":"xxxx","dateOfBirth":"xxxx","address":{"firstLine":"xxxx","town":"xxxx","country":"United Kingdom"},"metadata":{"trusted":true,"rating":"10","notes":"xxxx","other":{"shouldBeUnmasked":"Example multi-level-nested-value","shouldBeMasked":"xxxx"}}}""",
            maskedRecord.getValue()
        );
    }

    private String sampleValue() {
        return """
            {
               "specialId": 123,
               "status": "ACTIVE",
               "name": "John Smith",
               "dateOfBirth": "01-01-1991",
               "address": {
                 "firstLine": "123 Example Avenue",
                 "town": "Faketown",
                 "country": "United Kingdom"
               },
               "metadata": {
                 "trusted": true,
                 "rating": "10",
                 "notes": "All in good order"
               }
            }
            """;
    }

    private String sampleValueWithMultilevelNestedValues() {
        return """
            {
               "specialId": 123,
               "status": "ACTIVE",
               "name": "John Smith",
               "dateOfBirth": "01-01-1991",
               "address": {
                 "firstLine": "123 Example Avenue",
                 "town": "Faketown",
                 "country": "United Kingdom"
               },
               "metadata": {
                 "trusted": true,
                 "rating": "10",
                 "notes": "All in good order",
                 "other": {
                    "shouldBeUnmasked": "Example multi-level-nested-value",
                    "shouldBeMasked": "Example multi-level-nested-value"
                 }
               }
            }
            """;
    }
}
