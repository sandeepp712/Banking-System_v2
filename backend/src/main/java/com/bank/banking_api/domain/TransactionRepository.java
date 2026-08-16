package com.bank.banking_api.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {
    void save(Transaction transaction);
    boolean existsByIdempotencyKey(String key);
    Optional<Transaction> findByIdempotencyKey(String key);
    void update(Transaction transaction);
    List<Transaction> findByUserId(UUID userId);
}