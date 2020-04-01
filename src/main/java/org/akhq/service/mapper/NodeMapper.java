package org.akhq.service.mapper;

import org.akhq.models.Config;
import org.akhq.models.LogDir;
import org.akhq.models.Node;
import org.akhq.service.dto.node.ConfigDTO;
import org.akhq.service.dto.node.ConfigDTO.DataType;
import org.akhq.service.dto.node.LogDTO;
import org.akhq.service.dto.node.NodeDTO;

import javax.inject.Singleton;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class NodeMapper {
    private static final String CONFIG_FORMAT = "configs[%s]";

    public NodeDTO fromNodeToNodeDTO(Node node) {
        return new NodeDTO(node.getId(), node.getHost(), node.getPort(), node.getRack());
    }

    public ConfigDTO fromConfigToConfigDTO(Config config) {
        DataType dataType;
        try {
            switch (config.getName().substring(config.getName().lastIndexOf("."))) {
                case ".ms":
                    dataType = DataType.MILLI;
                    break;
                case ".size":
                    dataType = DataType.BYTES;
                    break;
                default:
                    dataType = DataType.TEXT;
                    break;
            }
        } catch (StringIndexOutOfBoundsException ex) {
            dataType = DataType.TEXT;
        }

        return new ConfigDTO(config.getName(), config.getValue(), config.getDescription(),
                ConfigDTO.Source.valueOf(config.getSource().name()), dataType, config.isReadOnly(),
                config.isSensitive());
    }

    public LogDTO fromLogDirToLogDTO(LogDir logDir) {
        return new LogDTO(logDir.getBrokerId(), logDir.getTopic(), logDir.getPartition(), logDir.getSize(),
                logDir.getOffsetLag());
    }

    public Map<String, String> convertConfigsMap(Map<String, String> configs) {
        return configs.entrySet().stream().collect(Collectors.toMap(
                e -> String.format(CONFIG_FORMAT, e.getKey()),
                Map.Entry::getValue
        ));
    }
}
