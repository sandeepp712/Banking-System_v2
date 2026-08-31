package com.bank.banking_api.persistence;

import com.bank.banking_api.domain.RefreshToken;
import com.bank.banking_api.domain.RefreshTokenRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcRefreshTokenRepository implements RefreshTokenRepository {
    private final JdbcTemplate jdbcTemplate;

    public RowMapper<RefreshToken> refreshTokenRowMapper = (rs, rowNum) -> {
        UUID id = rs.getObject("id", UUID.class);
        UUID userId = rs.getObject("user_id", UUID.class);
        String tokenHash = rs.getString("token_hash");
        String deviceInfo = rs.getString("device_info");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant expiredAt = rs.getTimestamp("expired_at").toInstant();
        Boolean revoke = rs.getBoolean("revoked");
        Instant revokedAt = rs.getTimestamp("revoked_at") != null ? rs.getTimestamp("revoked_at").toInstant() : null;

        return RefreshToken.builder().id(id).userId(userId).tokenHash(tokenHash).deviceInfo(deviceInfo).createdAt(createdAt).expiredAt(expiredAt).revoked(revoke).revokedAt(revokedAt).build();
    };

    public JdbcRefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public void save(RefreshToken refreshToken) {
        String sql = """
                Insert INTO refresh_tokens (id,user_id,token_hash,device_info,created_at,expired_at,revoked,revoked_at)
                VALUES (?,?,?,?,?,?,?,?)
                """;

        java.sql.Timestamp createdAt = java.sql.Timestamp.from(refreshToken.getCreatedAt());
        java.sql.Timestamp expiredAt = java.sql.Timestamp.from(refreshToken.getExpiredAt());
        java.sql.Timestamp revokedAt = refreshToken.getRevokedAt() != null
                ? java.sql.Timestamp.from(refreshToken.getRevokedAt())
                : null;

        jdbcTemplate.update(sql,
                refreshToken.getId(),
                refreshToken.getUserId(),
                refreshToken.getTokenHash(),
                refreshToken.getDeviceInfo(),
                createdAt,
                expiredAt,
                refreshToken.isRevoked(),
                revokedAt
        );
    }

    public Optional<RefreshToken> findByToken(String tokenHash) {
        String sql = "select * from refresh_tokens where token_hash = ?";
        try {
            RefreshToken token = jdbcTemplate.queryForObject(sql, refreshTokenRowMapper, tokenHash);
            return Optional.ofNullable(token);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean isValidToken(String tokenHash) {
        String sql = """
                Select count(*) from refresh_tokens where token_hash=? AND revoked= false AND expired_at > NOW()
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tokenHash);
        return count != null && count > 0;
    }

    public void revoke(RefreshToken refreshToken) {
        String sql = "Update refresh_tokens set revoked=true,revoked_at = Now() where token_hash=?";
        jdbcTemplate.update(sql, refreshToken.getTokenHash());
    }

    public void revokeAll(UUID userId) {
        String sql = "Update refresh_tokens set revoked=true, revoked_at=Now() where user_id = ? AND  revoked = false";
        jdbcTemplate.update(sql, userId);
    }
}