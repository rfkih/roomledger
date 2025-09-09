package com.roomledger.app.exthandler;

public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String msg) {
        super(msg);
    }
}