package com.bank.banking_api.domain;

import java.util.Optional;

public interface TransactionRepository {
    void save(Transaction transaction);
    boolean existsByIdempotencyKey(String key);
    Optional<Transaction> findByIdempotencyKey(String key);
    void update(Transaction transaction);
}