package org.kafkahq.service.dto.connect;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectDefinitionDTO {
    private String name;
    private String config;
    private String type;
    private String shortClassName;
    private List<TaskDefinitionDTO> tasks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskDefinitionDTO {
        private String name;
        private int id;
        private String state;
    }
}
