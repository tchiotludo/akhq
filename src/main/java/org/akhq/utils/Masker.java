package org.akhq.utils;

import org.akhq.models.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Masker {

    Logger LOG = LoggerFactory.getLogger(Masker.class);

    Record maskRecord(Record record);
}
