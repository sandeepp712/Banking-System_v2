package com.bank.banking_api.dto;

import java.math.BigDecimal;

public record WithdrawRequest(
        BigDecimal amount,
        String idempotency
) {
    public BigDecimal getAmount() {
        return amount;
    }

    public String getIdempotencyKey() {
        return idempotency;
    }
}