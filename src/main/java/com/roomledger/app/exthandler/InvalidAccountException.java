package com.roomledger.app.exthandler;

public class InvalidAccountException extends RuntimeException {
    public InvalidAccountException(String message) {
        super(message);
    }

    public InvalidAccountException(Exception e) {
        super(e);
    }
}