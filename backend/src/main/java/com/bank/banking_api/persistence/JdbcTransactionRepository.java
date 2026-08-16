package com.bank.banking_api.persistence;

import com.bank.banking_api.domain.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcTransactionRepository implements TransactionRepository {
    private final JdbcTemplate jdbcTemplate;

    public RowMapper<Transaction> rowMapper = (rs, rowNum) -> {
        UUID id = rs.getObject("id", UUID.class);
        TransactionType type = TransactionType.valueOf(rs.getString("type"));
        String fromAccount = rs.getString("from_account");
        String toAccount = rs.getString("to_account");
        BigDecimal amount = rs.getBigDecimal("amount");
        Currency currency = Currency.getInstance(rs.getString("currency"));
        TransactionStatus status = TransactionStatus.valueOf(rs.getString("status"));
        String idempotencyKey = rs.getString("idempotency_key");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        String responseCache = rs.getString("response_cache");
        String errorMessage = rs.getString("error_message");
        Instant completedAt = rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null;

        Money money = Money.of(amount, currency);

        return Transaction.builder().transactionId(id).type(type).fromAccountId(fromAccount).toAccountId(toAccount).Amount(money).status(status).idempotencyKey(idempotencyKey).createdAt(createdAt).responseCache(responseCache).errorMessage(errorMessage).completedAt(completedAt).build();
    };


    public JdbcTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public void save(Transaction transaction) {

        String sql = """
                Insert into transactions (id, type,from_account,to_account ,amount,currency,status,idempotency_key,created_at,response_cache,error_message,completed_at) 
                values (?,?,?,?,?,?,?,?,?,?,?,?)
                """;

        String jsonCache = (transaction.getResponseCache() == null || transaction.getResponseCache().isBlank()) ? null : transaction.getResponseCache();


        jdbcTemplate.update(sql, transaction.getId(), transaction.getType(), transaction.getFromAccountId(), transaction.getToAccountId(), transaction.getAmount().getAmount(), transaction.getAmount().getCurrency().getCurrencyCode(), transaction.getStatus().name(), transaction.getIdempotencyKey(), java.sql.Timestamp.from(transaction.getCreatedAt()), jsonCache, transaction.getErrorMessage(), transaction.getCompletedAt() != null ? java.sql.Timestamp.from(transaction.getCompletedAt()) : null);

    }

    //Crucial for Idempotency!
    public boolean existsByIdempotencyKey(String key) {
        String sql = "Select count(*) from transactions where idempotency_key=?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, key);
        return count != null && count > 0;
    }

    // To find the idempotency jey
    public Optional<Transaction> findByIdempotencyKey(String key) {
        String sql = "Select * from transactions where idempotency_key=?";
        try {
            Transaction tx = jdbcTemplate.queryForObject(sql, rowMapper, key);
            return Optional.ofNullable(tx);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    // Update the status
    public void update(Transaction transaction) {
        System.out.println("UPDATING: key=" + transaction.getIdempotencyKey() + ", status=" + transaction.getStatus() + ", responseCache=" + transaction.getResponseCache());

        String sql = "UPDATE transactions SET status=?,response_cache=COALESCE(?,response_cache),error_message=?,completed_at=? where idempotency_key=?";

        String jsonCache = (transaction.getResponseCache() == null || transaction.getResponseCache().isBlank()) ? null : transaction.getResponseCache();
        jdbcTemplate.update(sql, transaction.getStatus().name(), jsonCache, transaction.getErrorMessage(), transaction.getCompletedAt() != null ? java.sql.Timestamp.from(transaction.getCompletedAt()) : null, transaction.getIdempotencyKey());
    }

    // All Transaction of user
    public List<Transaction> findByUserId(UUID userId) {
        String sql = """
                 SELECT t.* FROM transactions t
                        JOIN accounts a ON a.account_number = t.from_account OR a.account_number = t.to_account
                        WHERE a.user_id = ?
                        ORDER BY t.created_at DESC
                """;

        return jdbcTemplate.query(sql, rowMapper, userId);
    }
}