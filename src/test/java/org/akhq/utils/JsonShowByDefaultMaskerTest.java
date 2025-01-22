package org.akhq.utils;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@MicronautTest(environments = "json-show-by-default-data-masking")
class JsonShowByDefaultMaskerTest extends MaskerTestHelper {

    @Inject
    Masker masker;

    @Test
    void shouldUseJsonShowByDefaultMasker() {
        assertInstanceOf(JsonShowByDefaultMasker.class, masker);
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
            "{\"specialId\":123,\"status\":\"ACTIVE\",\"name\":\"xxxx\",\"dateOfBirth\":\"xxxx\",\"address\":{\"firstLine\":\"xxxx\",\"town\":\"xxxx\",\"country\":\"United Kingdom\"},\"metadata\":{\"trusted\":true,\"rating\":\"10\",\"notes\":\"All in good order\"}}",
            maskedRecord.getValue()
        );
    }

    @Test
    void shouldDoNothingForUndefinedTopic() {
        Record record = sampleRecord(
            "different-topic",
            "some-key",
            sampleValue()
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            sampleValue(),
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
            record,
            maskedRecord
        );
    }

    @Test
    void forNonJsonValueThatLooksLikeJsonValueShouldReturnItself() {
        Record record = sampleRecord(
            "users",
            "some-key",
            "{not a valid json}"
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            record,
            maskedRecord
        );
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
                {"specialId":123,"status":"ACTIVE","name":"xxxx","dateOfBirth":"xxxx","address":{"firstLine":"xxxx","town":"xxxx","country":"United Kingdom"},"metadata":{"trusted":true,"rating":"10","notes":"All in good order","other":{"shouldBeUnmasked":"Example multi-level-nested-value","shouldBeMasked":"xxxx"}}}""",
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
