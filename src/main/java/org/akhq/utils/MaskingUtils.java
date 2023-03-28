package org.akhq.utils;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.akhq.configs.DataMasking;
import org.akhq.configs.DataMaskingFilter;
import org.akhq.models.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MaskingUtils {
    private static final Logger LOG = LoggerFactory.getLogger(MaskingUtils.class);

    @Inject
    DataMasking dataMasking;

    public Record maskRecord(Record record) {
        LOG.trace("masking record");

        String value = record.getValue();
        String key = record.getKey();

        for (DataMaskingFilter filter : dataMasking.getFilters()) {
            if (value != null) {
                value = value.replaceAll(filter.getSearchRegex(), filter.getReplacement());
            }
            if (key != null) {
                key = key.replaceAll(filter.getSearchRegex(), filter.getReplacement());
            }
        }

        record.setValue(value);
        record.setKey(key);

        return record;
    }
}