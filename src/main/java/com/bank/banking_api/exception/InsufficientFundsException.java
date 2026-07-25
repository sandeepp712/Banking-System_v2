package com.bank.banking_api.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super("Insufficient funds: " + message);
    }
}