package org.akhq.utils;

import org.akhq.models.Record;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Masker {

    Logger LOG = LoggerFactory.getLogger(Masker.class);

    Record maskRecord(Record record);

    default boolean isTombstone(Record record) {
        return record.getValue() == null;
    }

    default boolean isJson(Record record) {
        try {
            new JSONObject(record.getValue());
        } catch (JSONException ex) {
            try {
                new JSONArray(record.getValue());
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
}
