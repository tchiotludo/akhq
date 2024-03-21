package org.akhq.models.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ConnectAuditEvent extends AuditEvent {

    private String kafkaClusterId;
    private String connectClusterId;
    private String connectorName;
    private ActionType actionType;
    private Integer taskId;

    public static ConnectAuditEvent newConnector(String kafkaClusterId, String connectClusterId, String connectorName) {
        return new ConnectAuditEvent(kafkaClusterId, connectClusterId, connectorName, ActionType.CONNECT_CREATE, null);
    }

    public static ConnectAuditEvent updateConnector(String kafkaClusterId, String connectClusterId, String connectorName) {
        return new ConnectAuditEvent(kafkaClusterId, connectClusterId, connectorName, ActionType.CONNECT_UPDATE, null);
    }

    public static ConnectAuditEvent deleteConnector(String kafkaClusterId, String connectClusterId, String connectorName) {
        return new ConnectAuditEvent(kafkaClusterId, connectClusterId, connectorName, ActionType.CONNECT_DELETE, null);
    }

    public static ConnectAuditEvent pauseConnector(String kafkaClusterId, String connectClusterId, String connectorName) {
        return new ConnectAuditEvent(kafkaClusterId, connectClusterId, connectorName, ActionType.CONNECT_PAUSE, null);
    }

    public static ConnectAuditEvent resumeConnector(String kafkaClusterId, String connectClusterId, String connectorName) {
        return new ConnectAuditEvent(kafkaClusterId, connectClusterId, connectorName, ActionType.CONNECT_RESUME, null);
    }

    public static ConnectAuditEvent restartConnector(String kafkaClusterId, String connectClusterId, String connectorName) {
        return new ConnectAuditEvent(kafkaClusterId, connectClusterId, connectorName, ActionType.CONNECT_RESTART, null);
    }

    public static ConnectAuditEvent restartTaskConnector(String kafkaClusterId, String connectClusterId, String connectorName, int taskId) {
        return new ConnectAuditEvent(kafkaClusterId, connectClusterId, connectorName, ActionType.CONNECT_TASK_RESTART, taskId);
    }

    @Override
    public String getType() {
        return "CONNECT";
    }
}
