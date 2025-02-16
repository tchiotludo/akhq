package org.akhq.utils;

import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import static org.akhq.utils.MaskerTestHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

interface JsonMaskerTest {

    String SAMPLE_VALUE = """
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

    String SAMPLE_VALUE_WITH_MULTI_LEVEL_NESTED_OBJECTS = """
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

    String SAMPLE_VALUE_WITH_ARRAYS = """
            {
               "specialId": 123,
               "status": "ACTIVE",
               "name": "John Smith",
               "dateOfBirth": "01-01-1991",
               "address": [
                   {
                     "firstLine": "123 Example Avenue",
                     "town": "Faketown",
                     "country": "United Kingdom"
                   },
                   {
                     "firstLine": "Old Address",
                     "town": "Previoustown",
                     "country": "United Kingdom"
                   }
               ],
               "metadata": {
                 "trusted": true,
                 "rating": "10",
                 "notes": "All in good order",
                 "other": {
                    "shouldBeUnmasked": "Example multi-level-nested-value",
                    "shouldBeMasked": "Example multi-level-nested-value",
                    "arrayToMask": [ "one", "two", "three" ],
                    "arrayToShow": [ "one", "two", "three" ]
                 }
               }
            }
            """;

    Masker getMasker();

    @Test
    default void shouldMaskRecordValue() {
        Record record = sampleRecord(
            "users",
            "some-key",
            SAMPLE_VALUE
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            "{\"specialId\":123,\"status\":\"ACTIVE\",\"name\":\"xxxx\",\"dateOfBirth\":\"xxxx\",\"address\":{\"firstLine\":\"xxxx\",\"town\":\"xxxx\",\"country\":\"United Kingdom\"},\"metadata\":{\"trusted\":true,\"rating\":\"10\",\"notes\":\"All in good order\"}}",
            maskedRecord.getValue()
        );
    }

    @Test
    default void forTombstoneShouldReturnItself() {
        Record record = sampleRecord(
            "users",
            "some-key",
            null
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            record,
            maskedRecord
        );
    }

    @Test
    default void ifRecordHasMultiLevelNestedValuesShouldBeProcessedCorrectly() {
        Record record = sampleRecord(
            "users",
            "some-key",
            SAMPLE_VALUE_WITH_MULTI_LEVEL_NESTED_OBJECTS
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            """
                {"specialId":123,"status":"ACTIVE","name":"xxxx","dateOfBirth":"xxxx","address":{"firstLine":"xxxx","town":"xxxx","country":"United Kingdom"},"metadata":{"trusted":true,"rating":"10","notes":"All in good order","other":{"shouldBeUnmasked":"Example multi-level-nested-value","shouldBeMasked":"xxxx"}}}""",
            maskedRecord.getValue()
        );
    }

    @Test
    default void ifRecordUsesArrays_handlingOfFieldsWorksAsExpected() {
        Record record = sampleRecord(
            "users",
            "some-key",
            SAMPLE_VALUE_WITH_ARRAYS
        );
        Record maskedRecord = getMasker().maskRecord(record);
        assertEquals(
            """
            {"specialId":123,"status":"ACTIVE","name":"xxxx","dateOfBirth":"xxxx","address":[{"firstLine":"xxxx","town":"xxxx","country":"United Kingdom"},{"firstLine":"xxxx","town":"xxxx","country":"United Kingdom"}],"metadata":{"trusted":true,"rating":"10","notes":"All in good order","other":{"shouldBeUnmasked":"Example multi-level-nested-value","shouldBeMasked":"xxxx","arrayToMask":["xxxx","xxxx","xxxx"],"arrayToShow":["one","two","three"]}}}""",
            maskedRecord.getValue()
        );
    }
}
