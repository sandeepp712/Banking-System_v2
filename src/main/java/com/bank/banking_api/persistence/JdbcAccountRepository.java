package com.bank.banking_api.persistence;

import com.bank.banking_api.domain.Account;
import com.bank.banking_api.domain.AccountRepository;
import com.bank.banking_api.domain.Money;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAccountRepository implements AccountRepository {
    private final JdbcTemplate jdbcTemplate;

    public RowMapper<Account> rowMapper = (rs, rowNum) -> {
        UUID id = rs.getObject("id", UUID.class);
        String accountNumber = rs.getString("account_number");
        BigDecimal amount = rs.getBigDecimal("balance_amount");
        String currency = rs.getString("balance_currency");
        String status = rs.getString("status");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();

        Money balance = Money.of(amount, Currency.getInstance(currency));

        return new Account(id, accountNumber, balance, status, createdAt, updatedAt);
    };

    public JdbcAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Account accounts) {
        String sql = """
                INSERT INTO accounts (id, account_number, balance_amount, balance_currency, status, created_at, updated_at) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(sql,
                accounts.getId(),
                accounts.getAccountNumber(),
                accounts.getBalance().getAmount(),      // Unpack Money to BigDecimal
                accounts.getBalance().getCurrency().getCurrencyCode(),    //  Unpack Money to String
                accounts.getStatus(),
                java.sql.Timestamp.from(accounts.getCreatedAt()),
                java.sql.Timestamp.from(accounts.getUpdatedAt())

        );
    }

    // To find the account number
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "Select * from accounts where account_number = ?";
        return jdbcTemplate.query(sql, rowMapper, accountNumber).stream().findFirst();
    }

    // To find the account number with exclusive locks
    public Optional<Account> findByAccountNumberForUpdate(String accountNumber) {
        String sql = "Select * from accounts where account_number = ? FOR UPDATE";
        List<Account> accounts = jdbcTemplate.query(sql, rowMapper, accountNumber);
        return accounts.stream().findFirst();
    }

    @Transactional
    public void update(Account accounts) {
        String sql = "Update accounts SET balance_amount=?,updated_at=? where account_number=?";
        jdbcTemplate.update(sql, accounts.getBalance().getAmount(), java.sql.Timestamp.from(accounts.getUpdatedAt()), accounts.getAccountNumber());
    }

    public List<Account> findAll() {
        String sql = "Select * from accounts ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public void delete(String accountNumber) {
        String sql = "Delete from accounts where account_number = ?";
        jdbcTemplate.update(sql, accountNumber);
    }
}