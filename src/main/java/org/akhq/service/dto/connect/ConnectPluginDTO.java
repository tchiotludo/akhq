package org.akhq.service.dto.connect;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectPluginDTO {
    private String className;
    private String type;
    private String version;
    private List<DefinitionDTO> definitions;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefinitionDTO {
        private String name;
        private String type;
        private boolean required;
        private String defaultValue;
        private String importance;
        private String documentation;
        private String group;
        private String width;
        private String displayName;
        private Collection<String> dependents;
        private int order;
    }
}
