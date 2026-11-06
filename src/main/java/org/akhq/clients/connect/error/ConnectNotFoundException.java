package org.akhq.clients.connect.error;

public class ConnectNotFoundException extends ConnectRestException {
    public ConnectNotFoundException(String message) {
        super(404, message);
    }
}

