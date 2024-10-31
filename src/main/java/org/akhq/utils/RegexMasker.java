package org.akhq.utils;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.akhq.configs.DataMasking;
import org.akhq.configs.RegexFilter;
import org.akhq.models.Record;

import java.util.List;

@Singleton
// For backwards compatibility and not wanting to break things for existing users, this is the default masker.
@Requires(property = "akhq.security.data-masking.mode", value = "regex", defaultValue = "regex")
public class RegexMasker implements Masker {

    private final List<RegexFilter> filters;

    public RegexMasker(DataMasking  dataMasking ) {
        this.filters = dataMasking.getFilters();
    }    @Override
    public Record maskRecord(Record record) {
        if(filters.isEmpty()){
            return record;
        }
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
