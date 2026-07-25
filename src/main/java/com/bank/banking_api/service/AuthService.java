package com.bank.banking_api.service;

import com.bank.banking_api.domain.AccountRole;
import com.bank.banking_api.domain.User;
import com.bank.banking_api.persistence.UserRepository;
import com.bank.banking_api.security.CustomUserDetails;
import com.bank.banking_api.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void register(String username, String rawPassword, AccountRole role, Instant created_at) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists!");
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

    public String login(String username, String rawPassword) {
        //1. Check if the user exists Before attempting authenication
        if (!userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username does not exists! Please register with username: "+username);
        }

        //2. Proceed Authenication using Spring Security
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, rawPassword)
        );

        //2. If successful, generate token
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
//        assert userDetails != null;
        return jwtTokenProvider.generateToken(userDetails);

    }
}