package com.bank.banking_api.dto;

import java.math.BigDecimal;

public record DepositRequest(
        BigDecimal amount,
        String idempotencyKey) {

    public BigDecimal getAmount() {
        return amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}