package org.akhq.modules.schemaregistry.apicurio;

/**
 * Bidirectional mapping between AKHQ's flat subject string and Apicurio Registry's
 * {@code (groupId, artifactId)} coordinates.
 *
 * <p>When no {@code configuredGroupId} is set on the connection:
 * <ul>
 *     <li>Artifacts in the {@code default} group are represented as {@code <artifactId>}.</li>
 *     <li>Artifacts in any other group are represented as {@code <groupId>:<artifactId>}.</li>
 * </ul>
 *
 * <p>When a {@code configuredGroupId} is set on the connection:
 * <ul>
 *     <li>Artifacts in that group are represented as {@code <artifactId>}.</li>
 *     <li>Subjects without a {@code :} separator resolve to {@code configuredGroupId}.</li>
 *     <li>Subjects with a {@code :} separator still resolve to the explicit {@code groupId:artifactId}.</li>
 * </ul>
 */
public record ApicurioSubjectCoordinates(String groupId, String artifactId) {
    public static final String DEFAULT_GROUP = "default";

    public static ApicurioSubjectCoordinates parse(String subject, String configuredGroupId) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject cannot be null or blank");
        }
        String fallbackGroup = normalizeGroup(configuredGroupId);
        int separator = subject.indexOf(':');
        if (separator > 0 && separator < subject.length() - 1) {
            String group = subject.substring(0, separator);
            String artifact = subject.substring(separator + 1);
            return new ApicurioSubjectCoordinates(normalizeGroup(group), artifact);
        }
        return new ApicurioSubjectCoordinates(fallbackGroup, subject);
    }

    public static String format(String groupId, String artifactId, String configuredGroupId) {
        String normalizedGroup = normalizeGroup(groupId);
        String defaultGroup = normalizeGroup(configuredGroupId);
        if (defaultGroup.equals(normalizedGroup)) {
            return artifactId;
        }
        return normalizedGroup + ":" + artifactId;
    }

    public static String normalizeGroup(String groupId) {
        return (groupId == null || groupId.isBlank()) ? DEFAULT_GROUP : groupId.trim();
    }
}
