package com.bank.banking_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "From account number is required")
        String fromAccountNumber,

        @NotBlank(message = "To account number is required")
        String toAccountNumber,

        @NotNull(message = "Amount is required")
        @Positive(message = "Transfer amount must be greater than zero")
        BigDecimal amount,
        String idempotencyKey
) {
    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }


    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}