package com.bank.banking_api.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Transaction {
    private final UUID transactionId;
    private final TransactionType type;
    private final String fromAccountId;
    private final String toAccountId;
    private final Money amount;
    private final TransactionStatus status;
    private final String idempotencyKey;
    private final Instant createdAt;
    private final String responseCache;
    private final String errorMessage;
    private final Instant completedAt;


    private Transaction(UUID transactionId, TransactionType type, String fromAccountId, String toAccountId, Money amount, TransactionStatus status, String idempotencyKey, Instant createdAt, String responseCache, String errorMessage, Instant completedAt) {
        this.transactionId = transactionId != null ? transactionId : UUID.randomUUID();
        this.type = type;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.status = status != null ? status : TransactionStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.responseCache = responseCache != null ? responseCache : "";
        this.errorMessage = errorMessage != null ? errorMessage : "";
        this.completedAt = completedAt;
    }


    //Getters
    public UUID getId() {
        return this.transactionId;
    }

    public String getType() {
        return this.type.toString();
    }

    public String getFromAccountId() {
        return this.fromAccountId;
    }

    public String getToAccountId() {
        return this.toAccountId;
    }

    public Money getAmount() {
        return this.amount;
    }

    public TransactionStatus getStatus() {
        return this.status;
    }

    public String getIdempotencyKey() {
        return this.idempotencyKey;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public String getResponseCache() {
        return this.responseCache;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public Instant getCompletedAt() {
        return this.completedAt;
    }


    //Helper: create copy with new status
    public Transaction withStatus(TransactionStatus newStatus) {
        return new Transaction(this.transactionId, this.type, this.fromAccountId, this.toAccountId, this.amount, newStatus, this.idempotencyKey, this.createdAt, this.responseCache, this.errorMessage, this.completedAt);
    }

    //Helper: create copy with response cache
    public Transaction withResponseCache(String responseJson) {
        return new Transaction(this.transactionId, this.type, this.fromAccountId, this.toAccountId, this.amount, this.status, this.idempotencyKey, this.createdAt, responseJson, this.errorMessage, this.completedAt);
    }

    public Transaction withCompletedAt(Instant completedAt) {
        return new Transaction(this.transactionId, this.type, this.fromAccountId, this.toAccountId, this.amount, this.status, this.idempotencyKey, this.createdAt, this.responseCache, this.errorMessage, completedAt);
    }

    public Transaction withErrorMessage(String errorMessage) {
        return new Transaction(this.transactionId, this.type, this.fromAccountId, this.toAccountId, this.amount, this.status, this.idempotencyKey, this.createdAt, this.responseCache, errorMessage, this.completedAt);
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID transactionId;
        private TransactionType type;
        private String fromAccountId;
        private String toAccountId;
        private Money amount;
        private TransactionStatus status = TransactionStatus.PENDING;
        private String idempotencyKey;
        private Instant createdAt;
        private String responseCache;
        private String errorMessage;
        private Instant completedAt;

        public Builder transactionId(UUID transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder fromAccountId(String fromAccountId) {
            this.fromAccountId = fromAccountId;
            return this;
        }

        public Builder toAccountId(String toAccountId) {
            this.toAccountId = toAccountId;
            return this;
        }

        public Builder Amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder responseCache(String responseCache) {
            this.responseCache = responseCache;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Transaction build() {
            return new Transaction(transactionId, type, fromAccountId, toAccountId, amount, status, idempotencyKey, createdAt, responseCache, errorMessage, completedAt);
        }
    }


    // equals & hashCode based on transactionId (unique) – but also include idempotencyKey for safety
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction trans)) return false;
        return transactionId.equals(trans.transactionId) && idempotencyKey.equals(trans.idempotencyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, idempotencyKey);
    }

    @Override
    public String toString() {
        return "Transaction{" + "transactionId='" + transactionId + '\'' + ", from=" + fromAccountId + ", to=" + toAccountId + ", amount=" + amount + ", status=" + status + ", idempotencyKey='" + idempotencyKey + ", createdAt=" + createdAt + ", responseCache='" + responseCache + '\'' + ", errorMessage='" + errorMessage + '\'' + '}';
    }
}
