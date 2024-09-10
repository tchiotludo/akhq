package org.akhq.configs;

public enum DataMaskingMode {
    // Use the existing regex-based filtering
    REGEX,
    // Use filtering where by default all fields of all records are masked, with fields to unmask defined in allowlists
    JSON_MASK_BY_DEFAULT,
    // Use filtering where by default no fields of any records are masked, with fields to mask explicitly denied
    JSON_SHOW_BY_DEFAULT,
    // No masker at all, best performance
    NONE
}
