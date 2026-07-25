package com.bank.banking_api.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import com.bank.banking_api.config.RSAKeyConfig;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private final long jwtExpirationInMs = 900_000;
    private final RSAKeyConfig rsaKeyConfig;

    // Inject our config bean
    public JwtTokenProvider(RSAKeyConfig rsaKeyConfig) {
        this.rsaKeyConfig = rsaKeyConfig;
    }

    // For SIGNING provide Private key
    public RSAPrivateKey getSigningKey() {
        return rsaKeyConfig.getPrivateKey();
    }

    //For SIGNING provide Public key
    public RSAPublicKey getVerificationKey() {
        return rsaKeyConfig.getPublicKey();
    }


    /**
     * Generates a JWT for the given user details.
     * Claims: sub (userId), username, role, iat, exp.
     */
    public String generateToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);


        return Jwts.builder()
                .subject(userDetails.getUserId().toString())
                .claim("username", userDetails.getUsername())
                .claim("role", userDetails.getRole())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }


    /**
     * Validates the token. Returns true if the token is correctly signed and not expired.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getVerificationKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Log the error in production
            return false;
        }
    }

    /**
     * Extracts the user ID (sub claim) from the token.
     */
    public UUID getUserIdFromToken(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(getVerificationKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

            return UUID.fromString(subject);
        } catch (JwtException e) {
            throw new JwtException("Invalid Token", e);
        }
    }

    /**
     * Extract the role from the token
     */
    public String getRoleFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getVerificationKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }


    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getVerificationKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("username", String.class);
    }
}