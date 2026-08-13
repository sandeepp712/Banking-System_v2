package com.bank.banking_api.domain;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository {
    void save(AuditLog auditLog);

    List<AuditLog> findById(UUID id);
}