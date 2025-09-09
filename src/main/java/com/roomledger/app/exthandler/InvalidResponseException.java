package com.roomledger.app.exthandler;

public class InvalidResponseException extends RuntimeException{
    public InvalidResponseException(String message) {
        super(message);
    }
}
