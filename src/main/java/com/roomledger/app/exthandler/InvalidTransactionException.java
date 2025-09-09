package com.roomledger.app.exthandler;

public class InvalidTransactionException extends Exception {
    public InvalidTransactionException(String msg) {
        super(msg);
    }
}
