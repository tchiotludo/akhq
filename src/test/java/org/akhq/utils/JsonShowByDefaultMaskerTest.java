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

}
