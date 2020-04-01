package org.akhq.service.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicDataDTO {
    private List<RecordDTO> records;
    private String after;
    private long recordCount;
    //private int pageCount; // To be added when a way to correctly paginate topic data is found
}
