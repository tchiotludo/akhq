package org.akhq.service.dto.acls;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AclsDTO {
    private String user;
    private String principalEncoded;
    private String resource;
    private String host;
    private List<String> permissions;
}
