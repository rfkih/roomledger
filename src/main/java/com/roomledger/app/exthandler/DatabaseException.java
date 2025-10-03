package com.roomledger.app.exthandler;

import jakarta.validation.constraints.NotBlank;

public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, @NotBlank String s) {
        super(message);
    }
}

