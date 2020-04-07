package org.akhq.rest;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import org.akhq.service.ConnectService;
import org.akhq.service.dto.connect.ConnectDefinitionConfigsDTO;
import org.akhq.service.dto.connect.ConnectDefinitionDTO;
import org.akhq.service.dto.connect.ConnectPluginDTO;
import org.akhq.service.dto.connect.CreateConnectDefinitionDTO;
import org.akhq.service.dto.connect.DeleteConnectDefinitionDTO;
import org.akhq.service.dto.connect.ModifyConnectDefinitionStateDTO;
import org.akhq.service.dto.connect.RestartConnectDefinitionTaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@Controller("${akhq.server.base-path:}/api")
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

    @Get("/connect/definition")
    public ConnectDefinitionDTO fetchConnectDefinition(String clusterId, String connectId, String definitionId) {
        log.debug("Fetching definition: {}", definitionId);
        return connectService.getConnectDefinition(clusterId, connectId, definitionId);
    }

    @Get("/connect/definition/configs")
    public ConnectDefinitionConfigsDTO fetchConnectDefinitionConfigs(String clusterId, String connectId, String definitionId) {
        log.debug("Fetching connect {} definiton {} configs", connectId, definitionId);
        return connectService.getConnectDefinitionConfigs(clusterId, connectId, definitionId);
    }

    @Get("/connect/definitions")
    public List<ConnectDefinitionDTO> fetchConnectDefinitions(String clusterId, String connectId) {
        log.debug("Fetching definitions for connect: {}", connectId);
        return connectService.getConnectDefinitions(clusterId, connectId);
    }

    @Post("/connect/definition/create")
    public void createConnectDefinition(@Body CreateConnectDefinitionDTO createConnectDefinitionDTO) {
        log.debug(
                "Creating definition {} on connect: {}",
                createConnectDefinitionDTO.getName(),
                createConnectDefinitionDTO.getConnectId()
        );
        connectService.addConnectDefinition(createConnectDefinitionDTO);
    }

    @Put("/connect/definition/pause")
    public void pauseConnectDefinition(ModifyConnectDefinitionStateDTO modifyConnectDefinitionStateDTO) {
        log.debug(
                "Pausing definition {} from connect: {}",
                modifyConnectDefinitionStateDTO.getDefinitionId(),
                modifyConnectDefinitionStateDTO.getConnectId()
        );
        connectService.pauseConnectDefinition(
                modifyConnectDefinitionStateDTO.getClusterId(),
                modifyConnectDefinitionStateDTO.getConnectId(),
                modifyConnectDefinitionStateDTO.getDefinitionId()
        );
    }

    @Put("/connect/definition/resume")
    public void resumeConnectDefinition(ModifyConnectDefinitionStateDTO modifyConnectDefinitionStateDTO) {
        log.debug(
                "Resuming definition {} from connect: {}",
                modifyConnectDefinitionStateDTO.getDefinitionId(),
                modifyConnectDefinitionStateDTO.getConnectId()
        );
        connectService.resumeConnectDefinition(
                modifyConnectDefinitionStateDTO.getClusterId(),
                modifyConnectDefinitionStateDTO.getConnectId(),
                modifyConnectDefinitionStateDTO.getDefinitionId()
        );
    }

    @Put("/connect/definition/restart")
    public void restartConnectDefinition(ModifyConnectDefinitionStateDTO modifyConnectDefinitionStateDTO) {
        log.debug(
                "Restart definition {} from connect: {}",
                modifyConnectDefinitionStateDTO.getDefinitionId(),
                modifyConnectDefinitionStateDTO.getConnectId()
        );
        connectService.restartConnectDefinition(
                modifyConnectDefinitionStateDTO.getClusterId(),
                modifyConnectDefinitionStateDTO.getConnectId(),
                modifyConnectDefinitionStateDTO.getDefinitionId()
        );
    }

    @Put("/connect/definition/task/restart")
    public void restartConnectDefinitionTask(RestartConnectDefinitionTaskDTO restartConnectDefinitionTaskDTO) {
        log.debug(
                "Restarting task {} from definition {} from connect {}",
                restartConnectDefinitionTaskDTO.getTaskId(),
                restartConnectDefinitionTaskDTO.getDefinitionId(),
                restartConnectDefinitionTaskDTO.getConnectId()
        );
        connectService.restartTask(
                restartConnectDefinitionTaskDTO.getClusterId(),
                restartConnectDefinitionTaskDTO.getConnectId(),
                restartConnectDefinitionTaskDTO.getDefinitionId(),
                restartConnectDefinitionTaskDTO.getTaskId()
        );
    }

    @Delete("/connect/delete")
    public List<ConnectDefinitionDTO> deleteConnectDefinition(DeleteConnectDefinitionDTO deleteConnectDefinitionDTO) {
        log.debug(
                "Deleting definition {} from connect {}",
                deleteConnectDefinitionDTO.getDefinitionId(),
                deleteConnectDefinitionDTO.getConnectId()
        );
        return this.connectService.deleteConnectDefinition(deleteConnectDefinitionDTO);
    }

    @Get("/connect/plugins")
    public List<ConnectPluginDTO> fetchConnectPlugins(String clusterId, String connectId) {
        log.debug("Fetching plugins for connect: {}", connectId);
        return connectService.getConnectPlugins(clusterId, connectId);
    }
}
