package com.bank.banking_api.service;

import com.bank.banking_api.domain.AccountRole;
import com.bank.banking_api.domain.RefreshToken;
import com.bank.banking_api.domain.RefreshTokenRepository;
import com.bank.banking_api.domain.User;
import com.bank.banking_api.exception.AuthenicationException;
import com.bank.banking_api.exception.UserAlreadyExistsException;
import com.bank.banking_api.persistence.UserRepository;
import com.bank.banking_api.security.CustomUserDetails;
import com.bank.banking_api.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDetailsService userDetailsService;
    private static final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, RefreshTokenRepository refreshTokenRepository, UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userDetailsService = userDetailsService;

    }

    public void register(String username, String rawPassword, AccountRole role, Instant created_at) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists!");
        }

        // For password complexity
        if (rawPassword.length() < 8 || !rawPassword.matches(".*[A-Z].*") ||
                !rawPassword.matches(".*[0-9].*") || !rawPassword.matches(".*[^a-zA-Z0-9].*")) {
            throw new IllegalArgumentException("Password must contain at least 8 characters with uppercase, number and special characters.");
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        UUID id = UUID.randomUUID();
        User user = new User(id, username, encodedPassword, role, created_at);
        userRepository.save(user);
    }

    public void login(String username, String rawPassword, HttpServletRequest req, HttpServletResponse res) {
        //1. Check if the user exists Before attempting authenication
//        if (!userRepository.findByUsername(username).isPresent()) {
//            throw new IllegalArgumentException("Username does not exists! Please register with username: "+username);
//        }

        //2. Proceed Authenication using Spring Security
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, rawPassword)
        );

        //2. If successful, generate token
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        String deviceInfo = req.getHeader("User-Agent");
        if (deviceInfo == null || deviceInfo.isBlank()) {
            deviceInfo = "Unknown-Device";
        }

        issueRefreshToken(userDetails.getUserId(), deviceInfo, res);
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        issueAccessToken(accessToken,res);
    }


    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new AuthenicationException("Refresh token is empty!");
        }

        String hashedToken = DigestUtils.sha256Hex(refreshToken);

        Optional<RefreshToken> token = refreshTokenRepository.findByToken(hashedToken);
        if (token.isEmpty()) {
            throw new AuthenicationException("Invalid refresh token!");
        }

        RefreshToken tokenEntity = token.get();
        UUID userId = tokenEntity.getUserId();

        if (tokenEntity.isRevoked()) {
            refreshTokenRepository.revokeAll(userId);
            logger.warn("Token reuse detected for user:{}. All session revoked", userId);
            throw new AuthenicationException("Token reuse detected for user!");
        }

        if (tokenEntity.getExpiredAt().isBefore(Instant.now())) {
            throw new AuthenicationException("Refresh Token expired!");
        }

        // Rotate Refresh token
        refreshTokenRepository.revoke(tokenEntity);
        issueRefreshToken(userId, tokenEntity.getDeviceInfo(), response);

        // Issue new access token cookie
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(userId.toString());
        String newAccessToken = jwtTokenProvider.generateToken(userDetails);
        issueAccessToken(newAccessToken,response);
    }


    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = extractRefreshTokenFromCookie(request);
        if (rawRefreshToken != null) {
            String hashedToken = DigestUtils.sha256Hex(rawRefreshToken);
            refreshTokenRepository.findByToken(hashedToken).ifPresent(refreshTokenRepository::revoke);
        }

        //Clear Refresh token
        clearCookie("REFRESH_TOKEN",response);

        //Clear Access token
        clearCookie("JWT_TOKEN",response);
    }


    //Helper method
    private void issueAccessToken(String accessToken, HttpServletResponse response) {
        ResponseCookie cookie=ResponseCookie.from("JWT_TOKEN",accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void issueRefreshToken(UUID userId, String deviceInfo, HttpServletResponse res) {
        byte[] randomeBytes = new byte[64];
        random.nextBytes(randomeBytes);
        String rawRefreshToken = Base64.encodeBase64String(randomeBytes);
        String hashedRefreshToken = DigestUtils.sha256Hex(rawRefreshToken);

        RefreshToken newToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(hashedRefreshToken)
                .deviceInfo(deviceInfo)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .revoked(false)
                .revokedAt(null)
                .build();

        refreshTokenRepository.save(newToken);

        ResponseCookie cookie = ResponseCookie.from("REFRESH_TOKEN", rawRefreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(30))
                .sameSite("None")
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }


    private void clearCookie(String cookieName, HttpServletResponse res) {
        ResponseCookie cookie = ResponseCookie.from(cookieName,"")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }


    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookie = request.getCookies();
        if (cookie != null) {
            for (Cookie cookie1 : cookie) {
                if (cookie1.getName().equals("REFRESH_TOKEN")) {
                    return cookie1.getValue();
                }
            }
        }
        return null;
    }
}