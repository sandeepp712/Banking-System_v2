
### To-Do Task
1. In Auth service I have to implement the refresh token 
2.Rate limit for distributed system using redis




# Authentication Implementation Design (JWT + RSA)

## 1. Overview

This document outlines the complete authentication implementation for the Banking API, built using **Spring Boot**, **Spring Security**, **JWT (RS256)**, and **JDBC**.
The system supports user registration, login, JWT issuance, and rate limiting on authentication endpoints. It uses **asymmetric RSA** for signing and verifying JWTs.

## 2. Architecture & Flow

The system follows a **stateless** architecture. No HTTP sessions are stored. Each request carries a JWT that the server validates.

### Request Flow (Login)

1. **Client** → Sends `POST /api/v1/auth/login` with username/password.
2. **RateLimiterFilter** → Checks if the IP has exceeded the request limit (5 req/min).
3. **AuthController** → Delegates to `AuthService`.
4. **AuthenticationManager** → Invokes `CustomUserDetailsService.loadUserByUsername()`.
5. **PasswordEncoder (BCrypt)** → Verifies the raw password against the stored hash.
6. **JwtTokenProvider** → Generates a JWT signed with the RSA Private Key.
7. **Client** → Receives the JWT in the response.

### Request Flow (Protected Endpoint)

1. **Client** → Sends `Authorization: Bearer <token>`.
2. **RateLimiterFilter** → Skips check (only active for `/api/v1/auth/**`).
3. **JwtAuthenticationFilter** → Extracts token, validates signature using RSA Public Key, and sets `SecurityContext`.
4. **Controller** → Processes the request with authenticated user context.

---

## 3. Core Components

### 3.1 Domain Model: `User`
A plain Java domain object representing the database record.

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `UUID` | Primary key |
| `username` | `String` | Unique login ID |
| `passwordHash` | `String` | BCrypt encoded password |
| `role` | `AccountRole` | Enum (`ROLE_USER`, `ROLE_ADMIN`) |
| `createdAt` | `LocalDateTime` | Timestamp of registration |

### 3.2 Security Wrapper: `CustomUserDetails`
Implements `UserDetails`. Wraps the `User` domain object to integrate with Spring Security.

### 3.3 User Details Service: `CustomUserDetailsService`
Implements `UserDetailsService`. Loads the `User` from the database via `UserRepository` and returns a `CustomUserDetails` instance.

### 3.4 Data Access: `UserRepository`
Uses `JdbcTemplate` to query the `users` table.
- `Optional<User> findByUsername(String username)`
- `void save(User user)`

### 3.5 RSA Key Loading: `RSAKeyConfig`
Loads the RSA public/private keys from environment variables (`JWT_PUBLIC_KEY`, `JWT_PRIVATE_KEY`).
- Decodes Base64 strings.
- Generates Java `RSAPublicKey` and `RSAPrivateKey` instances using `KeyFactory`.
- Annotated with `@PostConstruct` to initialize on startup.

### 3.6 JWT Core: `JwtTokenProvider`
Manages JWT creation and validation using the `jjwt` library (0.12.x).

| Method | Description |
| :--- | :--- |
| `generateToken(CustomUserDetails user)` | Creates a JWT with claims (`sub`=user UUID, `username`, `role`, `iat`, `exp`). <br> **Signs with RSA Private Key.** |
| `validateToken(String token)` | Verifies the signature (RSA Public Key) and checks expiration. |
| `getUsernameFromToken(String token)` | Extracts the `username` claim. |
| `getUserIdFromToken(String token)` | Extracts the `sub` claim (UUID). |

### 3.7 Authentication Service: `AuthService`
Contains the business logic for registration and login.
- `register()`: Hashes password using `BCryptPasswordEncoder`, saves user.
- `login()`: Uses `AuthenticationManager` to authenticate. If successful, calls `JwtTokenProvider.generateToken()`.

### 3.8 REST Controller: `AuthController`
Exposes public endpoints.
- `POST /api/v1/auth/register` -> Returns `200 OK` with success message.
- `POST /api/v1/auth/login` -> Returns `200 OK` with JWT.

---

## 4. Security Configuration (`SecurityConfig`)

The security filter chain is configured to be stateless and filter-specific.

```java
http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .anyRequest().authenticated()
    )
    .addFilterBefore(rateLimiterFilter, JwtAuthenticationFilter.class)
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);