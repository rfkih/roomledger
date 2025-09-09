package com.roomledger.app.exthandler;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(Exception e) {
        super(e);
    }
}
