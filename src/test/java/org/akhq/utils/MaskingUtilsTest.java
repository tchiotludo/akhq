package org.akhq.utils;

import io.micronaut.context.ApplicationContext;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskingUtilsTest {

    @Test
    void shouldMasksRecordValue() {
        ApplicationContext ctx = ApplicationContext.run("data-masking");
        MaskingUtils maskingUtils = ctx.getBean(MaskingUtils.class);

        Record record = new Record();
        record.setValue("{\"secret-key\":\"my-secret-value\"}");

        Record maskedRecord = maskingUtils.maskRecord(record);

        assertEquals(
            "{\"secret-key\":\"xxxx\"}",
            maskedRecord.getValue()
        );

        ctx.close();
    }

    @Test
    void shouldMasksRecordKey() {
        ApplicationContext ctx = ApplicationContext.run("data-masking");
        MaskingUtils maskingUtils = ctx.getBean(MaskingUtils.class);

        Record record = new Record();
        record.setKey("{\"secret-key\":\"my-secret-value\"}");

        Record maskedRecord = maskingUtils.maskRecord(record);

        assertEquals(
            "{\"secret-key\":\"xxxx\"}",
            maskedRecord.getKey()
        );

        ctx.close();
    }

    @Test
    void shouldReturnGroupsToAllowPartialMasking() {
        ApplicationContext ctx = ApplicationContext.run("data-masking");
        MaskingUtils maskingUtils = ctx.getBean(MaskingUtils.class);

        Record record = new Record();
        record.setValue("{\"some-key\":\"+12092503766\"}");

        Record maskedRecord = maskingUtils.maskRecord(record);

        assertEquals(
            "{\"some-key\":\"+120925xxxx\"}",
            maskedRecord.getValue()
        );

        ctx.close();
    }
}