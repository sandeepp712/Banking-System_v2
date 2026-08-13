package com.bank.banking_api.exception;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String message) {
        super("Duplicate transaction: " + message);
    }
}