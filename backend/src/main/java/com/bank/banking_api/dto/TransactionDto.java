package com.bank.banking_api.dto;

import com.bank.banking_api.domain.Money;
import com.bank.banking_api.domain.TransactionStatus;

import java.time.Instant;
import java.util.UUID;

public record TransactionDto(
        UUID transactionId,
        String fromAccount,
        String toAccount,
        Money amount,
        TransactionStatus status,
        Instant createdAt,
        Instant completedAt
) {}