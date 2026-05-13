package org.akhq.clients.connect.error;

public class ConnectRestException extends RuntimeException {
    private final int statusCode;

    public ConnectRestException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ConnectRestException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

