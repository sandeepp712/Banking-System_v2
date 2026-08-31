package com.bank.banking_api.controller;

import com.bank.banking_api.domain.AccountRole;
import com.bank.banking_api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.login(request.username(), request.password(), httpRequest, httpResponse);
        return ResponseEntity.ok(Map.of("message", "Login successfully!"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.refresh(httpRequest, httpResponse);
        return ResponseEntity.ok(Map.of("message", "Token Refresh successfully!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok().body(Map.of("message", "Logout successful!"));
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