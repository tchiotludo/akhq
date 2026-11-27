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
        String name1 = stripGroupPrefix(s1.getSubject());
        String name2 = stripGroupPrefix(s2.getSubject());
        boolean exact1 = this.defaultSchemaName.equals(name1);
        boolean exact2 = this.defaultSchemaName.equals(name2);
        if (exact1 != exact2) {
            return exact1 ? -1 : 1;
        }
        boolean prefix1 = name1.startsWith(topicName);
        boolean prefix2 = name2.startsWith(topicName);
        if (prefix1 != prefix2) {
            return prefix1 ? -1 : 1;
        }
        return s1.getSubject().compareTo(s2.getSubject());
    }

    private static String stripGroupPrefix(String subject) {
        int colon = subject.indexOf(':');
        return colon >= 0 && colon < subject.length() - 1 ? subject.substring(colon + 1) : subject;
    }

}
