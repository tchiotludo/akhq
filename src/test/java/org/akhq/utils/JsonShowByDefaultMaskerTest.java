package org.akhq.utils;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.Getter;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import static org.akhq.utils.MaskerTestHelper.sampleRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Getter
@MicronautTest(environments = "json-show-by-default-data-masking")
class JsonShowByDefaultMaskerTest implements JsonMaskerTest {

    @Inject
    JsonShowByDefaultMasker masker;

    @Test
    public void forUndefinedTopicShouldDefaultShowAllValues() {
        Record record = sampleRecord(
            "different-topic",
            "some-key",
            SAMPLE_VALUE
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            SAMPLE_VALUE,
            maskedRecord.getValue()
        );
    }

    @Test
    public void forTopicMatchingRegexFilterShouldOnlyMaskMatchedKeys() {
        Record record = sampleRecord(
            "user-events-audit",
            "some-key",
            SAMPLE_VALUE
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            "{\"specialId\":123,\"status\":\"ACTIVE\",\"name\":\"xxxx\",\"dateOfBirth\":\"01-01-1991\",\"address\":{\"firstLine\":\"123 Example Avenue\",\"town\":\"Faketown\",\"country\":\"United Kingdom\"},\"metadata\":{\"trusted\":true,\"rating\":\"10\",\"notes\":\"All in good order\"}}",
            maskedRecord.getValue()
        );
    }

    @Test
    public void forTopicMatchingMultipleRegexFiltersShouldMaskUnionOfKeys() {
        Record record = sampleRecord(
            "user-events-42",
            "some-key",
            SAMPLE_VALUE
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            "{\"specialId\":123,\"status\":\"ACTIVE\",\"name\":\"xxxx\",\"dateOfBirth\":\"xxxx\",\"address\":{\"firstLine\":\"123 Example Avenue\",\"town\":\"Faketown\",\"country\":\"United Kingdom\"},\"metadata\":{\"trusted\":true,\"rating\":\"10\",\"notes\":\"All in good order\"}}",
            maskedRecord.getValue()
        );
    }

    @Test
    public void topicMatchingShouldBeCaseInsensitive() {
        Record record = sampleRecord(
            "USERS",
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
    public void topicMatchingShouldRequireFullMatchNotPartialMatch() {
        Record record = sampleRecord(
            "users-archive",
            "some-key",
            SAMPLE_VALUE
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            SAMPLE_VALUE,
            maskedRecord.getValue()
        );
    }

    @Test
    public void filterWithoutTopicShouldBeIgnored() {
        Record record = sampleRecord(
            "unknown",
            "some-key",
            SAMPLE_VALUE
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            SAMPLE_VALUE,
            maskedRecord.getValue()
        );
    }

}
