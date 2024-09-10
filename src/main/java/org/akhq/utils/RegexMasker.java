package org.akhq.utils;

import lombok.RequiredArgsConstructor;
import org.akhq.configs.RegexFilter;
import org.akhq.models.Record;

import java.util.List;

@RequiredArgsConstructor
public class RegexMasker implements Masker {

    private final List<RegexFilter> filters;

    @Override
    public Record maskRecord(Record record) {
        String value = record.getValue();
        String key = record.getKey();

        for (RegexFilter filter : filters) {
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
