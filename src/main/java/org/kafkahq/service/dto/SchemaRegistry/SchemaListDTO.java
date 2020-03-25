package org.kafkahq.service.dto.SchemaRegistry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaListDTO {
    private List<SchemaDTO> list;
    private int totalPageNumber;
}
