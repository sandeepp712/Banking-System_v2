package com.bank.banking_api.controller;

import com.bank.banking_api.domain.AccountRole;
import com.bank.banking_api.security.CustomUserDetails;
import com.bank.banking_api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        //1. Authenticate the user
        String token = authService.login(request.username, request.password);

        //2. Create an HttpOnly cookie
        ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")                     //CSRF protection
                .build();

        //3. Add cookie to response
        response.addHeader("Set-Cookie", cookie.toString());

        //4. Return success (no token in body)
        return ResponseEntity.ok().body(Map.of("message", "Login successful!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

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