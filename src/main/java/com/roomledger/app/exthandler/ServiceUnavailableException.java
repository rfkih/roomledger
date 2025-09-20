package com.roomledger.app.exthandler;

public class ServiceUnavailableException extends Exception {
    public ServiceUnavailableException(String msg) {
        super(msg);
    }
}
