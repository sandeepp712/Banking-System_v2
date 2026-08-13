package com.bank.banking_api.persistence;

import com.bank.banking_api.domain.AuditLog;
import com.bank.banking_api.domain.AuditLogRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAuditLogRepository implements AuditLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public final RowMapper<AuditLog> rowMapper = (rs, rowNum) -> {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID transactionId = UUID.fromString(rs.getString("transaction_id"));
        UUID actorId = UUID.fromString(rs.getString("actor_id"));
        String action = rs.getString("action");
        String details = rs.getString("details");
        Timestamp timestamp = rs.getTimestamp("timestamp");

        return new AuditLog(id, transactionId, actorId, action, details, timestamp.toInstant());
    };

    public void save(AuditLog auditLog) {
        String sql = """
                insert into audit_logs (id,transaction_id,actor_id,action,details,timestamp) 
                values(?,?,?,?,?::jsonb,?)
                """;

        String jsonDetails = (auditLog.getDetails() == null || auditLog.getDetails().isBlank()) ? "" : auditLog.getDetails();

        jdbcTemplate.update(sql,
                auditLog.getId(),
                auditLog.getTransactionId(),
                auditLog.getActorId(),
                auditLog.getActionName(),
                jsonDetails,
                Timestamp.from(auditLog.getTimestamp())
        );
    }

    public List<AuditLog> findById(UUID actorId) {
        String sql = "select * from audit_logs where actor_id=? order by timestamp desc";
        return jdbcTemplate.query(sql, rowMapper, actorId);
    }
}