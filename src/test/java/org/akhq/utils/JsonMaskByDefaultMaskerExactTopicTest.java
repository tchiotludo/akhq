package org.akhq.utils;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.Getter;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import static org.akhq.utils.MaskerTestHelper.sampleRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Getter
@MicronautTest(environments = "json-mask-by-default-data-masking-exact-topics")
class JsonMaskByDefaultMaskerExactTopicTest implements JsonMaskerTest {

    private static final String FULLY_MASKED_VALUE =
        "{\"specialId\":\"xxxx\",\"status\":\"xxxx\",\"name\":\"xxxx\",\"dateOfBirth\":\"xxxx\",\"address\":{\"firstLine\":\"xxxx\",\"town\":\"xxxx\",\"country\":\"xxxx\"},\"metadata\":{\"trusted\":\"xxxx\",\"rating\":\"xxxx\",\"notes\":\"xxxx\"}}";

    @Inject
    public JsonMaskByDefaultMasker masker;

    @Test
    public void regexLookingTopicShouldBeTreatedLiterallyWhenRegexIsDisabled() {
        Record record = sampleRecord(
            "user-events-audit",
            "some-key",
            SAMPLE_VALUE
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            FULLY_MASKED_VALUE,
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
            FULLY_MASKED_VALUE,
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
            "{\"specialId\":\"xxxx\",\"status\":\"ACTIVE\",\"name\":\"xxxx\",\"dateOfBirth\":\"xxxx\",\"address\":{\"firstLine\":\"xxxx\",\"town\":\"xxxx\",\"country\":\"xxxx\"},\"metadata\":{\"trusted\":\"xxxx\",\"rating\":\"xxxx\",\"notes\":\"xxxx\"}}",
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
