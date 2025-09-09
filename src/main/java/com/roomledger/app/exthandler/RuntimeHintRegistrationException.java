package com.roomledger.app.exthandler;

public class RuntimeHintRegistrationException extends RuntimeException {
    public RuntimeHintRegistrationException(String message) {
        super(message);
    }

    public RuntimeHintRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
