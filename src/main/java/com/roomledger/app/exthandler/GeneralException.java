package com.roomledger.app.exthandler;

public class GeneralException extends RuntimeException {
    public GeneralException(String message) {
        super(message);
    }
    public GeneralException(Exception e) {
        super(e);
    }
}