package com.bank.banking_api.dto;

import com.bank.banking_api.domain.Money;
import com.bank.banking_api.domain.Transaction;
import com.bank.banking_api.domain.TransactionStatus;

import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        String fromAccountId,
        String toAccountId,
        Money amount,
        TransactionStatus status,
        Instant create_at,
        String responseCache,
        String error_message,
        Instant completed_at
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                transaction.getResponseCache(),
                transaction.getErrorMessage(),
                transaction.getCompletedAt()
        );
    }
}