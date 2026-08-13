package com.bank.banking_api.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;

import com.bank.banking_api.config.RSAKeyConfig;
import com.bank.banking_api.exception.JwtTokenExpiredException;
import com.bank.banking_api.exception.JwtTokenInvalidException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private final long jwtExpirationInMs = 900_000;  // 15 minutes
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

    // Single parse method: extract claims or throws the correct authenticationExceptions
    public Claims extractAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getVerificationKey())
                    .build()
                    .parseClaimsJws(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtTokenExpiredException("Jwt token has expired");
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenInvalidException("Invalid JWT token");
        }
    }

    public UUID getUserIdFromToken(String token) {
        String subject = extractAllClaimsFromToken(token).getSubject();
        return UUID.fromString(subject);
    }


//    /**
//     * Validates the token. Returns true if the token is correctly signed and not expired.
//     */
//    public boolean validateToken(String token) {
//        try {
//            Jwts.parser()
//                    .verifyWith(getVerificationKey())
//                    .build()
//                    .parseSignedClaims(token);
//            return true;
//        } catch (io.jsonwebtoken.ExpiredJwtException e) {
//            // throws AuthenticationException -> spring security returns 401
//            throw new JwtTokenExpiredException("JWT token has expired");
//        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
//            // throws AuthenicationException -> srping security returns 401
//            throw new JwtTokenInvalidException("Invalid JWT token");
//        }
//    }
//
//    /**
//     * Extracts the user ID (sub claim) from the token.
//     */
//    public UUID getUserIdFromToken(String token) {
//        try {
//            String subject = Jwts.parser()
//                    .verifyWith(getVerificationKey())
//                    .build()
//                    .parseSignedClaims(token)
//                    .getPayload()
//                    .getSubject();
//
//            return UUID.fromString(subject);
//        } catch (JwtException e) {
//            throw new JwtException("Invalid Token", e);
//        }
//    }
//
//    /**
//     * Extract the role from the token
//     */
//    public String getRoleFromToken(String token) {
//        return Jwts.parser()
//                .verifyWith(getVerificationKey())
//                .build()
//                .parseSignedClaims(token)
//                .getPayload()
//                .get("role", String.class);
//    }
//
//
//    public String getUsernameFromToken(String token) {
//        return Jwts.parser()
//                .verifyWith(getVerificationKey())
//                .build()
//                .parseSignedClaims(token)
//                .getPayload()
//                .get("username", String.class);
//    }
}