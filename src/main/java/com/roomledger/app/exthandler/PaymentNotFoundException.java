package com.roomledger.app.exthandler;

public class PaymentNotFoundException extends Exception {
    public PaymentNotFoundException(String msg) {
        super(msg);
    }
}