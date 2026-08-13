package com.bank.banking_api.domain;

import java.time.Instant;
import java.util.UUID;

public class AuditLog {
    private final UUID id;
    private final UUID transactionId;
    private final UUID actorId;
    private final String actionName;
    private final String details;
    private final Instant timestamp;


    public AuditLog(UUID id, UUID transactionId, UUID actorId, String actionName, String details, Instant timestamp) {
        this.id = id;
        this.transactionId = transactionId;
        this.actorId = actorId;
        this.actionName = actionName;
        this.details = details;
        this.timestamp = timestamp;
    }

    //Getters
    public UUID getId() {return id;}
    public UUID getTransactionId() {return transactionId;}
    public UUID getActorId() {return actorId;}
    public String getActionName() {return actionName;}
    public String getDetails() {return details;}
    public Instant getTimestamp() {return timestamp;}


    //Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID transactionId;
        private UUID actorId;
        private String actionName;
        private String details;
        private Instant timestamp;

        public Builder withId(UUID id) {this.id = id; return this;}
        public Builder withTransactionId(UUID transactionId) {this.transactionId = transactionId; return this;}
        public Builder withActorId(UUID actorId) {this.actorId = actorId; return this;}
        public Builder withActionName(String actionName) {this.actionName = actionName; return this;}
        public Builder withDetails(String details) {this.details = details; return this;}
        public Builder withTimestamp(Instant timestamp) {this.timestamp = timestamp; return this;}

        public AuditLog build() {
            return new AuditLog(id, transactionId, actorId, actionName, details, timestamp);
        }
    }
}