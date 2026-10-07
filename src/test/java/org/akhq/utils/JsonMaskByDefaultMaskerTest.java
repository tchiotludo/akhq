package org.akhq.utils;

import com.google.gson.JsonParser;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.configs.DataMasking;
import org.akhq.models.Record;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.akhq.utils.MaskerTestHelper.sampleRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Getter
@MicronautTest(environments = "json-mask-by-default-data-masking")
class JsonMaskByDefaultMaskerTest implements JsonMaskerTest {

    @Inject
    public JsonMaskByDefaultMasker masker;

    @Test
    public void forNonJsonValueShouldReturnDisclaimer() {
        Record record = sampleRecord(
            "users",
            "some-key",
            "not a valid json"
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            "This record is unable to be masked as it is not a structured object. " +
                "This record is unavailable to view due to safety measures from json_mask_by_default to not leak " +
                "sensitive data.",
            maskedRecord.getValue()
        );
    }

    @Test
    public void forNonJsonValueThatLooksLikeJsonValueShouldReturnDisclaimer() {
        Record record = sampleRecord(
            "users",
            "some-key",
            "{not a valid json}"
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            "This record is unable to be masked as it is not a structured object. " +
                "This record is unavailable to view due to safety measures from json_mask_by_default to not leak " +
                "sensitive data.",
            maskedRecord.getValue()
        );
    }

    @Test
    public void ifJsonParsingThrowsExceptionShouldReturnFalse() {
        Record record = sampleRecord(
            "different-topic",
            "some-key",
            SAMPLE_VALUE
        );

        try (MockedStatic<JsonParser> mockStatic = Mockito.mockStatic(JsonParser.class)) {
            mockStatic.when(() -> JsonParser.parseString(SAMPLE_VALUE)).thenThrow(new RuntimeException("Bad exception!"));
            Record record1 = getMasker().maskRecord(record);
            assertEquals("An exception occurred during an attempt to mask this record. This record is unavailable to view due to safety measures from json_mask_by_default to not leak sensitive data.", record1.getValue());
        }
    }

    @Test
    public void forUndefinedTopicShouldDefaultMaskAllValues() {
        Record record = sampleRecord(
            "different-topic",
            "some-key",
            SAMPLE_VALUE
        );

        Record maskedRecord = getMasker().maskRecord(record);

        assertEquals(
            "{\"specialId\":\"xxxx\",\"status\":\"xxxx\",\"name\":\"xxxx\",\"dateOfBirth\":\"xxxx\",\"address\":{\"firstLine\":\"xxxx\",\"town\":\"xxxx\",\"country\":\"xxxx\"},\"metadata\":{\"trusted\":\"xxxx\",\"rating\":\"xxxx\",\"notes\":\"xxxx\"}}",
            maskedRecord.getValue()
        );
    }

    @Test
    public void forWildcardShouldNotMaskAllValuesUnderThatPath() {
        Record record = sampleRecord(
            "wildcard",
            "some-key",
            SAMPLE_VALUE_WITH_ARRAYS
        );

        Record maskedRecord = getMasker().maskRecord(record);
        assertEquals(
            "{\"specialId\":\"xxxx\",\"status\":\"xxxx\",\"name\":\"xxxx\",\"dateOfBirth\":\"xxxx\",\"address\":[{\"firstLine\":\"123 Example Avenue\",\"town\":\"Faketown\",\"country\":\"United Kingdom\"},{\"firstLine\":\"Old Address\",\"town\":\"Previoustown\",\"country\":\"United Kingdom\"}],\"metadata\":{\"trusted\":\"xxxx\",\"rating\":\"xxxx\",\"notes\":\"xxxx\",\"other\":{\"shouldBeUnmasked\":\"Example multi-level-nested-value\",\"shouldBeMasked\":\"Example multi-level-nested-value\",\"arrayToMask\":[\"one\",\"two\",\"three\"],\"arrayToShow\":[\"one\",\"two\",\"three\"],\"jsonArray\":[{\"showThis\":\"Hello\",\"maskThis\":\"World\"},{\"showThis\":\"How are\",\"maskThis\":\"You\"}]}}}",
            maskedRecord.getValue()
        );
    }

    @Test
    public void forRootWildcardShouldNotMaskAnything() {
        Record record = sampleRecord(
            "unmask-all",
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
