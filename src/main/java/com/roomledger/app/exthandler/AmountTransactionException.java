package com.roomledger.app.exthandler;

public class AmountTransactionException extends RuntimeException {

    public AmountTransactionException(String message) {
        super(message);
    }

    public AmountTransactionException(Exception e) {
        super(e);
    }
}
