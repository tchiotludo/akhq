package org.kafkahq.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.kafkahq.models.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClusterDTO {

    private String id;

}
