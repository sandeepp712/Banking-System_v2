package com.bank.banking_api.dto;

import java.math.BigDecimal;

public record CreateAccountRequest(
        String accountNumber,
        BigDecimal amount
){

    public String getAccountNumber(){
        return accountNumber;
    }

    public BigDecimal getAmount(){
        return amount;
    }

}