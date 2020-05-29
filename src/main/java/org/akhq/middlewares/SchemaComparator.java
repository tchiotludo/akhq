package org.akhq.middlewares;

import org.akhq.models.Schema;

import java.util.Comparator;

public class SchemaComparator implements Comparator<Schema> {

    public static final String KEY_SUFFIX = "-key";
    public static final String VALUE_SUFFIX = "-value";

    private String topicName;
    private String defaultSchemaName;

    public SchemaComparator(String topicName, boolean isKey) {
        this.topicName = topicName;
        this.defaultSchemaName = this.topicName + (isKey ? KEY_SUFFIX : VALUE_SUFFIX);
    }

    @Override
    public int compare(Schema s1, Schema s2) {
        if (this.defaultSchemaName.equals(s1.getSubject())) {
            return -1;
        } else if (this.defaultSchemaName.equals(s2.getSubject())) {
            return 1;
        } else if (s1.getSubject().startsWith(topicName)) {
            return -1;
        } else if (s2.getSubject().startsWith(topicName)) {
            return 1;
        }
        return s1.getSubject().compareTo(s2.getSubject());
    }

}
