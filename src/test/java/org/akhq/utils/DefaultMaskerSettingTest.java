package org.akhq.utils;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@MicronautTest
public class DefaultMaskerSettingTest {

    @Inject
    Masker masker;

    @Test
    void defaultValuesShouldUseRegexForBackwardsCompatibility() {
        assertInstanceOf(RegexMasker.class, masker);
    }
}
