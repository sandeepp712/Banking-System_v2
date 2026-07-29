package com.bank.banking_api.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super("You do not have permission to access." + message);
    }
}