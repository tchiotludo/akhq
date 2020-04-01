package org.akhq.service.dto.connect;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConnectDefinitionDTO {
    private String clusterId;
    private String connectId;
    private  String name;
    private  Map<String, String> configs;
}
