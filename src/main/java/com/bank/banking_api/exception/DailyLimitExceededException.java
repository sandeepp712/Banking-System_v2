package com.bank.banking_api.exception;

public class DailyLimitExceededException extends RuntimeException {
    public DailyLimitExceededException(String message) {
        super("Daily limit exceeded: " + message);
    }
}