package com.roomledger.app.exthandler;

public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(Exception e) {
        super(e);
    }

    public DatabaseException(String message, DataAccessException cause) {
        super(message, cause);
    }

}

