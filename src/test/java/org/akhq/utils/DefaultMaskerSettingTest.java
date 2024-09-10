package org.akhq.utils;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.configs.DataMasking;
import org.junit.jupiter.api.Test;

import static org.akhq.configs.DataMaskingMode.REGEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@MicronautTest
public class DefaultMaskerSettingTest {

    @Inject
    DataMasking dataMasking;

    @Inject
    Masker masker;

    @Test
    void defaultValuesShouldUseRegexForBackwardsCompatibility() {
        assertEquals(
            REGEX,
            dataMasking.getMode()
        );
        assertInstanceOf(RegexMasker.class, masker);
    }
}
