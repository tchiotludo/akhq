package org.akhq.utils;

import org.akhq.models.Record;

public class NoOpMasker implements Masker {

    @Override
    public Record maskRecord(Record record) {
        return record;
    }
}
