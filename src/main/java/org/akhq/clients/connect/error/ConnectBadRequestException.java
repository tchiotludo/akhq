package org.akhq.clients.connect.error;

public class ConnectBadRequestException extends ConnectRestException {
    public ConnectBadRequestException(String message) {
        super(400, message);
    }
}

