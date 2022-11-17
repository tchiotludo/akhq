package org.akhq.modules;

public class InvalidClusterException extends RuntimeException {
    public InvalidClusterException(String message) {
        super(message);
    }
}
