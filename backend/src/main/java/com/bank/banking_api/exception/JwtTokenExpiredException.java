package com.bank.banking_api.exception;

import org.springframework.security.core.AuthenticationException;

public class JwtTokenExpiredException extends AuthenticationException{
    public JwtTokenExpiredException(String msg) {
        super(msg);
    }
}