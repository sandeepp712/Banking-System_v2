package com.bank.banking_api.persistence;

import com.bank.banking_api.domain.AccountRole;
import com.bank.banking_api.domain.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    // ✅ Wrapped in curly braces and added the "return" statement
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        UUID id = rs.getObject("id", UUID.class);
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");

        String roleStr = rs.getString("role");
        AccountRole role = AccountRole.valueOf(roleStr);

        // Safely map timestamp to Instant
        java.time.Instant createdAt = rs.getTimestamp("created_at").toInstant();

        // ✅ Construct and return the User domain object
        return new User(id, username, passwordHash, role, createdAt);
    };

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, username);
            List<User> users = jdbcTemplate.query(sql, userRowMapper, username);

            return users.stream().findFirst();
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(User user) {
        String sql = "INSERT INTO users (id, username, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)";

        // Converting Instant to Timestamp is safer for cross-database JDBC drivers
        Timestamp createdAtTimestamp = Timestamp.from(user.getCreated_at().toInstant(ZoneOffset.UTC));

        jdbcTemplate.update(sql,
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole().name(),
                createdAtTimestamp
        );
    }

    public void delete(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        jdbcTemplate.update(sql, username);
    }
}