package org.akhq.service.mapper;

import org.akhq.configs.Connect;
import org.akhq.service.dto.ConnectDTO;

import javax.inject.Singleton;

@Singleton
public class ConnectMapper {

    public ConnectDTO fromConnectToConnectDTO(Connect connect) {
        return new ConnectDTO(connect.getName(), connect.getUrl());
    }

}
