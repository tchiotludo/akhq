package org.kafkahq.rest;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import org.kafkahq.service.ConnectService;
import org.kafkahq.service.dto.connect.ConnectDefinitionDTO;
import org.kafkahq.service.dto.connect.DeleteConnectDefinitionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@Controller("${kafkahq.server.base-path:}/api")
public class ConnectResource {

    private final Logger log = LoggerFactory.getLogger(ConnectResource.class);

    private ConnectService connectService;

    @Inject
    public ConnectResource(ConnectService connectService) {
        this.connectService = connectService;
    }

    @Get("/connects")
    public List<String> fetchAllConnectNames(String clusterId) {
        log.debug("Fetch all connects from cluster {}", clusterId);
        return connectService.getAllConnectNames(clusterId);
    }

    @Get("/connect/definitions")
    public List<ConnectDefinitionDTO> fetchConnectDefinitions(String clusterId, String connectId) {
        log.debug("Fetching definitions for connect: {}", connectId);
        return connectService.getConnectDefinitions(clusterId, connectId);
    }

    @Delete("/connect/delete")
    public void deleteConnectDefinition(DeleteConnectDefinitionDTO deleteConnectDefinitionDTO) {
        log.debug(
                "Deleting definition {} from connect {}",
                deleteConnectDefinitionDTO.getDefinitionId(),
                deleteConnectDefinitionDTO.getConnectId()
        );
        this.connectService.deleteConnectDefinition(deleteConnectDefinitionDTO);
    }
}
