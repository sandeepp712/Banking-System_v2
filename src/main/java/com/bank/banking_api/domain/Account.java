package com.bank.banking_api.domain;

import java.time.Instant;
import java.util.UUID;

public class Account{
    private final UUID id;                  // Internal DB ID
    private final String accountNumber;     // User-facing ID
    private Money balance;                  // Immutable Money object
    private String status;                  // ACTIVE, FROZEN
    private final Instant createdAt;
    private Instant updatedAt;

    public final UUID ownerId;

    // Constructor for creating a NEW account
    public Account(String accountNumber, Money initialBalance, UUID ownerId) {
        this.id = UUID.randomUUID();        // Generate new UUID
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.ownerId = ownerId;
    }

    // Constructor for REBUILDING an account from the Database
    public Account(UUID id, String accountNumber, Money balance, String status, Instant createdAt, Instant updatedAt, UUID ownerId) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.ownerId = ownerId;
    }


    // --- Getters ---
    public UUID getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public Money getBalance() { return balance; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getOwnerId() { return ownerId; }

    // --- Business Logic (The Engine) ---
    public void credit(Money amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    public void debit(Money amount) {
        if (this.balance.isLessThan(amount)) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }
}