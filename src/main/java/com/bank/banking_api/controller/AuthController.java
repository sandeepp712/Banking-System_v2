package com.bank.banking_api.controller;

import com.bank.banking_api.domain.AccountRole;
import com.bank.banking_api.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        AccountRole role;

        try {
            role = AccountRole.valueOf(request.role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new RegisterResponse(request.username, null, "Invalid role. Allowed: " + java.util.Arrays.toString(AccountRole.values())));
        }

        authService.register(request.username, request.password, role, java.time.Instant.now());

        return ResponseEntity.ok(new RegisterResponse(request.username, role, "User registered successfully!"));
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.username, request.password);

        return ResponseEntity.ok(new LoginResponse(token, "Bearer "));
    }


    //DTO (Records)
    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String role
    ) {
    }

    public record RegisterResponse(
            String username,
            AccountRole role,
            String message
    ) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(
            String token,
            String tokenType) {
    }
}