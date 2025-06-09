# Proposal: Audit Log Enhancements for AKHQ

**Status:** Proposed
**Author:** AI Agent
**Date:** $(date +%Y-%m-%d)

## Abstract

This proposal details significant enhancements to AKHQ's logging capabilities by introducing a comprehensive audit log feature. While the current access log captures HTTP requests, it lacks the granularity needed for a true audit trail of administrative actions and critical changes within Kafka environments managed by AKHQ. The proposed enhancements include granular action logging, dedicated audit log configuration, integration with external logging systems, and an optional UI for audit log review. These changes aim to improve security, aid compliance efforts, simplify troubleshooting, and increase accountability.

## Current System

AKHQ currently provides an access log (`akhq.server.access-log`) which records incoming HTTP requests. This is useful for basic monitoring of who is accessing the AKHQ server. However, it does not provide a detailed audit trail for specific administrative actions performed *through* AKHQ on the Kafka resources themselves. For example, it's hard to distinguish between a user simply viewing a topic and a user deleting a topic or altering its configuration based solely on the access log.

## Proposed Enhancements

We propose the following enhancements to implement a robust audit logging system in AKHQ:

### 1. Granular Action Logging

*   **Description:** Log specific, meaningful actions performed by users via the AKHQ interface or API, going beyond simple HTTP request logging.
*   **Key Actions to Log (Examples):**
    *   **Topic Management:**
        *   Topic creation, deletion.
        *   Configuration changes (e.g., retention period, partition count, replication factor).
    *   **Consumer Group Management:**
        *   Offset resets (including details of the group, topic, partition, and new offset).
        *   Consumer group deletions.
    *   **Schema Registry (if configured):**
        *   Schema creation, updates, and deletions.
        *   Changes to schema compatibility settings.
    *   **Kafka Connect (if configured):**
        *   Connector creation, deletion.
        *   Connector pause/resume actions.
        *   Connector configuration updates.
    *   **Data Operations (Configurable):**
        *   Logging of data browsing or search queries made to topics, especially if topics may contain sensitive information. This should be configurable and potentially disabled by default due to log volume.
        *   Message production or deletion through AKHQ.
    *   **User Management & Permissions (if AKHQ implements its own user management):**
        *   Changes to user roles or permissions within AKHQ itself.
    *   **AKHQ Configuration Changes:**
        *   Modifications to AKHQ's own configuration if done via an API or UI (e.g., connection settings).
*   **Log Entry Details:** Each audit log entry should capture comprehensive information:
    *   **Timestamp:** Precise date and time of the action.
    *   **User ID:** Identifier of the user who performed the action (requires authentication to be enabled in AKHQ). If authentication is not enabled, a placeholder like "anonymous" or session ID could be used.
    *   **Source IP Address:** IP address from which the request originated.
    *   **Affected Kafka Cluster:** Identifier for the Kafka cluster targeted by the action.
    *   **Resource Type:** The type of resource being acted upon (e.g., `TOPIC`, `CONSUMER_GROUP`, `SCHEMA`, `CONNECTOR`, `ACL`).
    *   **Resource Name:** The specific name of the resource (e.g., topic name, group ID).
    *   **Action Performed:** A clear, standardized verb describing the action (e.g., `CREATE`, `DELETE`, `UPDATE_CONFIG`, `RESET_OFFSETS`, `PAUSE_CONNECTOR`).
    *   **Parameters/Details:** Key details of the action, where applicable. For configuration changes, this should ideally include both old and new values (or a delta). For offset resets, the specific topic-partition-offset.
    *   **Status:** Success or failure of the action. If failed, include an error message or code.

### 2. Dedicated Audit Log Configuration

*   **Description:** Introduce a new, distinct configuration section within `application.yml` specifically for audit logging, separate from the existing access log configuration.
*   **Proposed Configuration Section:** `akhq.audit`
*   **Configuration Options:**
    *   `enabled`: `true` or `false` to globally enable/disable audit logging.
    *   `default-log-level`: Default logging level for audit events (e.g., `INFO`).
    *   `log-format`: Format for the log entries (e.g., `JSON` (recommended for machine readability), `PLAIN_TEXT`, `CEF` (Common Event Format)).
    *   `outputs`: A list of outputs where audit logs should be sent (e.g., `FILE`, `CONSOLE`, `SYSLOG`, `ELASTICSEARCH`, `SPLUNK_HEC`).
    *   `actions-to-log`: Granular control over which categories of actions or specific actions are logged (see configuration example below). This helps manage log volume.
    *   Per-output configurations (e.g., file path and rotation for `FILE`, connection details for `ELASTICSEARCH`).

### 3. Integration with External Logging Systems

*   **Description:** Facilitate the forwarding of audit logs to centralized external logging systems for long-term storage, advanced analysis, alerting, and correlation with other system logs.
*   **Supported Integrations (Examples):**
    *   **Syslog:** Standard protocol for log forwarding.
    *   **ELK Stack:** Direct output to Elasticsearch or via Logstash.
    *   **Splunk:** Using Splunk HTTP Event Collector (HEC) or a universal forwarder.
    *   **Datadog:** Via Datadog agent or API.
    *   **Other SIEMs:** Support for generic formats like CEF or JSON over common protocols (HTTP, TCP) to allow integration with a wide range of SIEM tools.

### 4. Audit Log UI (Optional but Highly Recommended)

*   **Description:** Develop a dedicated section within the AKHQ user interface for viewing, searching, and filtering audit logs.
*   **Features:**
    *   Display of audit log entries in a readable format.
    *   **Search:** Full-text search capabilities.
    *   **Filtering:** Ability to filter logs by:
        *   Date/Time range
        *   User ID
        *   Affected Kafka cluster
        *   Resource type
        *   Resource name
        *   Action performed
        *   Status (Success/Failure)
    *   **Pagination:** For handling large volumes of log data.
    *   **Export (Optional):** Ability to export filtered log views (e.g., to CSV).
*   **Benefits:** Provides administrators with an accessible, built-in way to review audit trails without needing direct file system access or querying external systems, lowering the barrier to regular audit reviews.

## Benefits

*   **Enhanced Security:** Creates a detailed and immutable record of administrative actions, crucial for security investigations, detecting unauthorized access or changes, and responding to incidents.
*   **Compliance Adherence:** Helps organizations meet stringent audit trail requirements mandated by various regulations and standards (e.g., SOX, HIPAA, PCI-DSS, ISO 27001).
*   **Improved Troubleshooting:** Allows administrators to trace issues back to specific configuration changes or actions, understanding what changed, when, and by whom.
*   **Increased Accountability:** Establishes clear attribution for all critical operations performed on the Kafka infrastructure through AKHQ.
*   **Operational Insight:** Provides valuable data on how AKHQ is being used and which Kafka resources are most frequently managed.

## Proposed Configuration Example

```yaml
akhq:
  audit:
    enabled: true
    default-log-level: INFO # Default level for logged actions
    # Define one or more outputs
    outputs:
      # Log to a local file
      - type: FILE
        path: /var/log/akhq/audit.json # Recommend JSON for structured logging
        max-size: 100MB # Log rotation settings
        max-history: 10 # Number of old log files to keep
        log-format: JSON # Explicitly JSON for this output
      # Send to Elasticsearch
      # - type: ELASTICSEARCH
      #   url: "http://elasticsearch.example.com:9200"
      #   index-prefix: "akhq-audit" # e.g., akhq-audit-YYYY.MM.DD
      #   username: "user" # Optional
      #   password: "password" # Optional
      # Send to console (useful for Docker/Kubernetes environments)
      # - type: CONSOLE
      #   log-format: PLAIN_TEXT

    # Granular control over what actions are logged for each resource type.
    # If a resource type or action is not listed, it uses a default behavior
    # (e.g., log all actions if 'actions-to-log' is absent, or log none).
    # An empty array means log no actions for that resource type.
    actions-to-log:
      topic:
        - CREATE
        - DELETE
        - UPDATE_CONFIG
        - PRODUCE_MESSAGE # Example of data operation logging
      consumer-group:
        - DELETE
        - UPDATE_OFFSETS # Renamed from OFFSET_RESET for clarity
      schema:
        - CREATE
        - DELETE
        - UPDATE_COMPATIBILITY
      connect:
        - CREATE
        - DELETE
        - UPDATE_CONFIG
        - PAUSE
        - RESUME
      acl: # Kafka ACLs
        - CREATE
        - DELETE
      # For actions not explicitly listed, a global default could apply,
      # or they could be ignored if not specified.
      # default-logged-actions: [READ_CONFIG] # Example: log read operations by default
```

## Implementation Considerations

*   **Performance Impact:** Audit logging, especially if detailed and synchronous, can introduce performance overhead. Asynchronous logging and careful consideration of what to log (and at what detail level) are important.
*   **Log Storage Management:** For file-based logging, robust log rotation and retention policies are necessary. For external systems, ensure they are sized to handle the log volume.
*   **Security of Audit Logs:** Audit logs themselves contain sensitive information. Access to them must be controlled, and if stored in files, appropriate file permissions must be set. Encryption at rest for audit logs could be considered for highly sensitive environments.
*   **Standardization of Action Names:** Use a clear, consistent, and well-documented set of action names.
*   **Extensibility:** Design the audit log system to be extensible for new features or resources managed by AKHQ in the future.
*   **Backward Compatibility (Access Log):** The existing access log should continue to function as is, or users should be clearly informed of any changes or deprecation.
*   **User Context:** Ensuring accurate user identification is key. This relies heavily on AKHQ's authentication mechanisms.

## Conclusion

The proposed Audit Log Enhancements will transform AKHQ into a more secure, compliant, and manageable platform for Kafka operations. By providing detailed insights into administrative actions, it empowers organizations to better protect their data, meet regulatory obligations, and maintain stable and reliable Kafka services. This feature is considered essential for enterprise-grade deployments of AKHQ.
