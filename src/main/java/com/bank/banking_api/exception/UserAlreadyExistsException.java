package com.bank.banking_api.exception;

public class UserAlreadyExistsException extends RuntimeException{
    public UserAlreadyExistsException(String message){
        super("Username already exist: "+message);
    }
}