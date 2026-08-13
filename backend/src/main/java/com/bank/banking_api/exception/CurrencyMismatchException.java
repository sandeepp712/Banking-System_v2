package com.bank.banking_api.exception;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(String message) {
        super("Currency mismatch: " + message);
    }
}