# Proposal: Alerting and Notifications for AKHQ

**Status:** Proposed
**Author:** AI Agent
**Date:** $(date +%Y-%m-%d)

## Abstract

AKHQ excels at providing visibility and management capabilities for Apache Kafka clusters. However, it currently lacks a native, proactive alerting mechanism to inform users of potential issues or critical events. This proposal outlines the addition of a comprehensive Alerting and Notifications feature within AKHQ. This would allow users to define custom alert rules based on various Kafka metrics and states, and receive notifications through multiple channels. The goal is to enable faster issue detection and response, centralize Kafka monitoring, and improve operational efficiency.

## Current System

Currently, AKHQ users rely on external monitoring systems (e.g., Prometheus/Alertmanager, Datadog, Dynatrace) to set up alerts for their Kafka clusters. While AKHQ provides excellent diagnostic tools once an issue is suspected or known, it does not proactively identify and report these issues. This means users must either invest in separate monitoring solutions or risk slower detection of problems.

## Proposed Enhancements

We propose the following core components for the Alerting and Notifications feature:

### 1. Configurable Alert Rules

*   **Description:** Allow users to define and manage alert rules directly within AKHQ. These rules will monitor specific metrics and conditions within the Kafka ecosystem.
*   **Rule Definition Parameters:**
    *   **Name & Description:** User-friendly identifiers for the alert rule.
    *   **Cluster ID:** Option to target a specific Kafka cluster or all configured clusters.
    *   **Alert Type:** Predefined types of conditions to monitor.
    *   **Resource Specifier:** Regex or exact name for topics, consumer groups, connectors, etc.
    *   **Thresholds:** Configurable warning and critical thresholds for metric-based alerts.
    *   **Evaluation Interval:** How frequently the alert condition is checked.
    *   **Persistence/Debounce:** Configuration to prevent alert flapping (e.g., "alert if condition X persists for Y minutes").
    *   **Notification Channels:** Which configured channels should receive this alert.
*   **Examples of Alertable Conditions/Types:**
    *   **Consumer Group Health:**
        *   `CONSUMER_GROUP_LAG`: Alert when a consumer group's message lag (total or per partition) exceeds a defined threshold (in number of messages or estimated time).
        *   `CONSUMER_GROUP_NO_ACTIVE_MEMBERS`: Alert if a consumer group has no active members for a specified duration.
        *   `CONSUMER_GROUP_ERROR_RATE`: Alert if a consumer group shows a high rate of processing errors (requires error reporting from consumers).
    *   **Connector & Task Status (Kafka Connect):**
        *   `CONNECTOR_STATUS`: Alert when a connector enters a `FAILED` state.
        *   `TASK_STATUS`: Alert when one or more tasks for a connector enter a `FAILED` state.
    *   **Topic Metrics & Health:**
        *   `TOPIC_MESSAGE_RATE_LOW` / `TOPIC_MESSAGE_RATE_HIGH`: Alert if message ingress/egress for a topic falls below or exceeds defined thresholds.
        *   `TOPIC_SIZE_LIMIT`: Alert if a topic's size approaches its configured quota or disk limits (if AKHQ can infer these).
        *   `TOPIC_UNDER_REPLICATED_PARTITIONS`: Alert if a topic has under-replicated partitions for a sustained period.
        *   `TOPIC_NO_INCOMING_MESSAGES`: Alert if a topic has not received messages for a specified duration.
    *   **Broker Health (Potentially limited by Kafka AdminClient capabilities):**
        *   `BROKER_UNAVAILABLE`: Alert if a broker is not reachable by AKHQ or reported as down by the cluster. (Note: Often better handled by dedicated infrastructure monitoring, but AKHQ can offer a Kafka-centric perspective).
    *   **Schema Registry Health (if configured):**
        *   `SCHEMA_REGISTRY_UNAVAILABLE`: Alert if AKHQ cannot connect to the configured schema registry.
        *   `SCHEMA_COMPATIBILITY_ERROR`: Alert on attempts to register incompatible schemas (if AKHQ can monitor this).
    *   **Client Quotas:**
        *   `QUOTA_THRESHOLD_REACHED`: Alert when a client's produce/fetch/request quota is approaching or has exceeded defined limits (requires Kafka brokers to have quotas enabled and metrics available).
    *   **AKHQ Internal Alerts:**
        *   `AKHQ_CLUSTER_UNREACHABLE`: Alert if AKHQ loses connectivity to a configured Kafka cluster.

### 2. Notification Channels

*   **Description:** Integrate with a variety of popular notification channels to ensure alerts reach the appropriate users or teams promptly.
*   **Proposed Channels:**
    *   **Email:** Standard email notifications.
    *   **Slack:** Send messages to Slack channels or users.
    *   **Microsoft Teams:** Send messages to Teams channels.
    *   **PagerDuty:** Create incidents in PagerDuty.
    *   **Opsgenie:** Create alerts in Opsgenie.
    *   **Webhook:** A generic HTTP/S POST request to a user-defined URL, allowing integration with virtually any system that can receive webhooks (e.g., custom scripts, other ticketing systems).
*   **Configuration:** Global configuration for each channel type (e.g., SMTP server details for email, API keys) and per-alert rule selection of which channels to use.

### 3. Alert Management UI

*   **Description:** A dedicated section within the AKHQ user interface for managing the entire alerting lifecycle.
*   **Features:**
    *   **Rule Management:**
        *   Create, view, edit, enable/disable, and delete alert rules.
        *   Intuitive forms for defining rule parameters and thresholds.
    *   **Channel Configuration:**
        *   Securely add and manage configurations for different notification channels (e.g., entering API keys, webhook URLs, email distribution lists).
        *   Test buttons for each configured channel to verify integration.
    *   **Active Alerts Dashboard:**
        *   A view of currently firing alerts, their severity, and when they started.
    *   **Alert History:**
        *   A log of past alerts, their duration, and resolution status (if applicable).
    *   **Acknowledgement/Silencing:**
        *   Option to acknowledge an active alert (to indicate someone is looking into it).
        *   Option to temporarily silence an alert rule or a specific alert instance (e.g., during maintenance).

### 4. Thresholds, Severity, and Deduplication

*   **Description:** Provide fine-grained control over alert triggering and noise reduction.
*   **Severity Levels:** Allow users to assign severity levels (e.g., `INFO`, `WARNING`, `CRITICAL`) to alert rules. Notification channels could then be configured to handle different severities differently (e.g., PagerDuty for CRITICAL, Slack for WARNING).
*   **Multi-Level Thresholds:** Support for defining distinct thresholds for warning versus critical alerts (e.g., lag > 1000 is a WARNING, lag > 10000 is CRITICAL).
*   **Alert Debouncing/Flapping Prevention:** Mechanisms such as "alert only if condition persists for X evaluation cycles" or "do not re-notify for the same alert within Y minutes unless severity changes."
*   **Alert Summarization (Optional):** For very noisy alerts (e.g., many partitions of a consumer group lagging), an option to send a summary notification rather than individual alerts for each instance.

## Benefits

*   **Proactive Issue Detection & Resolution:** Enables operations teams to identify and address Kafka-related problems more quickly, minimizing potential downtime, data loss, or performance degradation.
*   **Centralized Kafka Operations:** Consolidates monitoring and alerting within the same platform used for Kafka management and browsing, reducing tool sprawl and context switching.
*   **Improved Operational Efficiency:** Automates the continuous monitoring of key Kafka health indicators and performance metrics, freeing up engineering time.
*   **Customizable & Contextual Alerts:** Allows users to tailor alerts precisely to their specific use cases, applications, and operational priorities.
*   **Enhanced Situational Awareness:** Provides a clearer, real-time understanding of the health and status of the Kafka infrastructure managed by AKHQ.
*   **Reduced Mean Time To Detect (MTTD):** By providing immediate notifications, AKHQ can significantly shorten the time it takes to become aware of issues.

## Proposed Configuration Example

```yaml
akhq:
  alerts:
    enabled: true
    # Default evaluation interval for rules if not specified in the rule itself
    default-evaluation-interval: "60s"
    # Global SMTP settings for email notifications
    smtp:
      host: "smtp.example.com"
      port: 587
      username: "akhq-alerts"
      password: "${SMTP_PASSWORD}" # Support for environment variable substitution
      from: "akhq@example.com"
      starttls-enabled: true

    rules:
      - name: "High Consumer Lag - Critical Order Processing"
        description: "Alerts when the critical order processing group's lag exceeds defined thresholds."
        cluster-id: "prod-kafka-west" # Target a specific cluster
        enabled: true
        type: CONSUMER_GROUP_LAG
        # Configuration specific to CONSUMER_GROUP_LAG
        consumer-group-regex: "^critical-order-processor$" # Exact match or regex
        # Lag can be defined in messages or time (e.g., "5m", "1h")
        # AKHQ would need to estimate time lag based on recent message rates if time is used.
        thresholds:
          - severity: WARNING
            min-lag-messages: 1000
            # Optional: min-lag-time: "2m"
            persist-for: "3m" # Condition must hold for 3 minutes
          - severity: CRITICAL
            min-lag-messages: 5000
            # Optional: min-lag-time: "10m"
            persist-for: "5m"
        notify: ["email-kafka-admins", "slack-critical-alerts", "pagerduty-sre-team"]
        # Optional: Custom message template for this alert
        # message-template: "ALERT: {{rule.name}} on {{cluster.name}}. Group {{group.name}} lag is {{lag.messages}}."

      - name: "Connector Failure - User Sync"
        description: "Alerts when the user synchronization connector or any of its tasks fail."
        cluster-id: "prod-kafka-east"
        enabled: true
        type: CONNECTOR_TASK_STATUS # Checks both connector and its tasks
        connector-name: "jdbc-source-user-sync"
        target-status: # Alert if status is one of these
          - FAILED
        # No specific thresholds needed, status is binary
        severity: CRITICAL
        notify: ["slack-data-integration-team"]

      - name: "Topic Under-Replicated - Core Services"
        description: "Alerts if any core services topics have under-replicated partitions."
        cluster-id: "prod-kafka-west"
        enabled: true
        type: TOPIC_UNDER_REPLICATED_PARTITIONS
        topic-regex: "^(auth-service|payment-service|inventory-service)-events$"
        persist-for: "10m" # Alert if URPs exist for 10 minutes
        severity: WARNING
        notify: ["email-kafka-admins"]

    notification-channels:
      # Email channel named 'email-kafka-admins'
      - name: "email-kafka-admins"
        type: EMAIL
        to:
          - "kafka-admins@example.com"
          - "oncall-sre@example.com"
        # Can override global SMTP settings here if needed

      # Slack channel named 'slack-critical-alerts'
      - name: "slack-critical-alerts"
        type: SLACK
        webhook-url: "${SLACK_CRITICAL_WEBHOOK_URL}" # From env var
        # Optional: channel: "#critical-alerts" (if webhook allows override)

      # Slack channel named 'slack-data-integration-team'
      - name: "slack-data-integration-team"
        type: SLACK
        webhook-url: "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX"

      # PagerDuty channel named 'pagerduty-sre-team'
      - name: "pagerduty-sre-team"
        type: PAGERDUTY
        integration-key: "${PAGERDUTY_INTEGRATION_KEY_SRE}" # From env var
        # Optional: custom event details, etc.
```

## Implementation Considerations

*   **Performance:** Alert condition evaluation, especially for many rules or complex metrics (like time-based lag estimation), could be resource-intensive. Efficient querying and caching strategies will be needed. Consider an internal scheduler for rule evaluation.
*   **State Management:** AKHQ will need to maintain the state of alerts (active, acknowledged, history). This might require a persistent store or careful in-memory management.
*   **Metric Collection:** AKHQ will need to collect or derive the necessary metrics from Kafka AdminClient, Consumer Group APIs, Kafka Connect REST API, and potentially JMX (though AdminClient is preferred).
*   **Extensibility:** Design the rule engine and notification system to be easily extensible with new alert types and notification channels in the future.
*   **User Experience (UX):** The UI for managing alerts and configuring channels should be intuitive and user-friendly.
*   **Security:** Securely store sensitive information like API keys for notification channels. Provide clear guidance on securing webhook endpoints.
*   **Testing:** Allow users to test alert rules and notification channel configurations (e.g., send a test alert).
*   **Documentation:** Comprehensive documentation will be essential for users to understand how to configure and use the alerting system effectively.

## Conclusion

Introducing a native Alerting and Notifications feature will significantly elevate AKHQ's capabilities from a management and visibility tool to a proactive operational platform for Apache Kafka. This will provide immense value to users by enabling faster issue detection, improving system reliability, and streamlining Kafka operations.
