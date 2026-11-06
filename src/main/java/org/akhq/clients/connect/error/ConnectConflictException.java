package org.akhq.clients.connect.error;

public class ConnectConflictException extends ConnectRestException {
    public ConnectConflictException(String message) {
        super(409, message);
    }
}

