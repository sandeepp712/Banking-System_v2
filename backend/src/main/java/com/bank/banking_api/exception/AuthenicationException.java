package com.bank.banking_api.exception;

public class AuthenicationException  extends RuntimeException{
    public AuthenicationException(String message) {
        super("You do not have token to access." + message);
    }
}