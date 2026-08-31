package com.bank.banking_api.domain;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository
{
    void save(RefreshToken refreshToken);
    Optional<RefreshToken> findByToken(String token);
    boolean isValidToken(String token);
    void revoke(RefreshToken refreshToken);
    void revokeAll(UUID id);
}