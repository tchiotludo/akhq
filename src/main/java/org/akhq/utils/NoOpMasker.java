package org.akhq.utils;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.akhq.models.Record;

@Singleton
@Requires(property = "akhq.security.data-masking.mode", value = "none")
public class NoOpMasker implements Masker {

    @Override
    public Record maskRecord(Record record) {
        return record;
    }
}
