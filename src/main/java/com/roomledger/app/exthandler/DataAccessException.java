package com.roomledger.app.exthandler;

public class DataAccessException extends RuntimeException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(Exception e) {
        super(e);
    }
}

