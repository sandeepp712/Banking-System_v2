package com.bank.banking_api.exception;

public class AccountFrozenException extends RuntimeException {
    public AccountFrozenException(String message) {
        super("Account frozen: " + message);
    }
}