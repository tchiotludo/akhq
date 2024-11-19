package org.akhq.utils;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import static org.akhq.utils.MaskerTestHelper.sampleRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@MicronautTest(environments = "regex-data-masking")
class RegexMaskerTest {

    @Inject
    Masker masker;

    @Test
    void shouldDefaultToRegexDataMasking() {
        assertInstanceOf(RegexMasker.class, masker);
    }

    @Test
    void shouldMasksRecordValue() {
        Record record = sampleRecord(
            "some-topic",
            "{\"secret-key\":\"my-secret-value\"}",
            "{\"secret-key\":\"my-secret-value\"}"
        );
        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            "{\"secret-key\":\"xxxx\"}",
            maskedRecord.getValue()
        );
    }

    @Test
    void shouldMasksRecordKey() {
        Record record = sampleRecord(
            "some-topic",
            "{\"secret-key\":\"my-secret-value\"}",
            "{\"secret-key\":\"my-secret-value\"}"
        );
        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            "{\"secret-key\":\"xxxx\"}",
            maskedRecord.getKey()
        );
    }

    @Test
    void shouldReturnGroupsToAllowPartialMasking() {
        Record record = sampleRecord(
            "some-topic",
            "{\"secret-key\":\"my-secret-value\"}",
            "{\"some-key\":\"+12092503766\"}"
        );
        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            "{\"some-key\":\"+120925xxxx\"}",
            maskedRecord.getValue()
        );
    }
}
