package com.bank.banking_api.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.TimeZone;

public class User {
    private UUID id;
    private String username;
    private String passwordHash;
    private AccountRole role;
    private LocalDateTime created_at;

    public User(UUID id, String username, String passwordHash, AccountRole role, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.created_at = LocalDateTime.ofInstant(createdAt,TimeZone.getDefault().toZoneId());
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountRole getRole() {
        return role;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }
}