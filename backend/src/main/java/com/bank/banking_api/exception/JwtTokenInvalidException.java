package com.bank.banking_api.exception;


import org.springframework.security.core.AuthenticationException;

public class JwtTokenInvalidException extends AuthenticationException {
    public JwtTokenInvalidException(String message) {
        super("Invalid JWT Token");
    }
}