package com.bank.banking_api.dto;

import java.math.BigDecimal;

public record AccountDto(String accountNumber, BigDecimal balance, String currency) {
}