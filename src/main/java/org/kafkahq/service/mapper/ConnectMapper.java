package org.kafkahq.service.mapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kafkahq.configs.Connect;
import org.kafkahq.service.dto.ConnectDTO;

import javax.inject.Singleton;
import java.net.URL;

@Singleton
public class ConnectMapper {

    public ConnectDTO fromConnectToConnectDTO(Connect connect) {
        return new ConnectDTO(connect.getName(), connect.getUrl());
    }

}
