package com.roomledger.app.exthandler;

import jakarta.validation.constraints.NotBlank;

public class InvalidTransactionException extends Exception {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
