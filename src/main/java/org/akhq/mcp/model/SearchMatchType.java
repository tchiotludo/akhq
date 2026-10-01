package org.akhq.mcp.model;

public enum SearchMatchType {
    CONTAINS("C"),
    EQUALS("E"),
    NOT_CONTAINS("N");

    private final String repositorySuffix;

    SearchMatchType(String repositorySuffix) {
        this.repositorySuffix = repositorySuffix;
    }

    public String repositorySuffix() {
        return repositorySuffix;
    }
}
