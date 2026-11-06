package org.akhq.utils;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.Getter;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import static org.akhq.utils.MaskerTestHelper.sampleRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Getter
@MicronautTest(environments = "json-show-by-default-data-masking-exact-topics")
class JsonShowByDefaultMaskerExactTopicTest implements JsonMaskerTest {

    @Inject
    public JsonShowByDefaultMasker masker;

    @Test
    public void regexLookingTopicShouldBeTreatedLiterallyWhenRegexIsDisabled() {
        Record record = sampleRecord(
            "user-events-audit",
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
    public void dotInTopicShouldNotActAsWildcardWhenRegexIsDisabled() {
        Record record = sampleRecord(
            "ordersXv1",
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
    public void topicWithDotShouldStillMatchExactly() {
        Record record = sampleRecord(
            "orders.v1",
            "some-key",
            SAMPLE_VALUE
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            "{\"specialId\":123,\"status\":\"xxxx\",\"name\":\"John Smith\",\"dateOfBirth\":\"01-01-1991\",\"address\":{\"firstLine\":\"123 Example Avenue\",\"town\":\"Faketown\",\"country\":\"United Kingdom\"},\"metadata\":{\"trusted\":true,\"rating\":\"10\",\"notes\":\"All in good order\"}}",
            maskedRecord.getValue()
        );
    }

    @Test
    public void exactTopicMatchingShouldBeCaseInsensitive() {
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
}
