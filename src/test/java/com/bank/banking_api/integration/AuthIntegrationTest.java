package com.bank.banking_api.integration;

import com.bank.banking_api.controller.AuthController;
import com.bank.banking_api.exception.AccessDeniedException;
import com.bank.banking_api.persistence.UserRepository;
import com.bank.banking_api.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class AuthIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    private RestClient restClient;

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        //we are telling spring: "use the jdbc url from the container"
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        //use the container username/password(they match "testuser/testpass"
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        //Explicitly tell spring to use the postgresSql driver
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE ");


        restClient = RestClient.create("http://localhost:" + port);
        rateLimiterService.reset();
    }


    @Test
    @DisplayName("Test Registeration Successfull")
    public void register_success_UserTest() {
        var request = new AuthController.RegisterRequest("testuser", "secreASDf23$@#te", "RETAIL_USER");
        var response = restClient.post()
                .uri("/api/v1/auth/register")
                .body(request)
                .retrieve()
                .toEntity(AuthController.RegisterResponse.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        var user = userRepository.findByUsername("testuser");
        assertTrue(user.isPresent());
        assertNotNull(user.get().getPasswordHash());
    }

    @Test
    @DisplayName("Test Duplicate Registration")
    public void register_duplicate_UserTest() {
        var request = new AuthController.RegisterRequest("testuser", "secreASDf23$@te", "RETAIL_USER");
        ResponseEntity<AuthController.RegisterResponse> firstResponse = restClient.post()
                .uri("/api/v1/auth/register")
                .body(request)
                .retrieve()
                .toEntity(AuthController.RegisterResponse.class);

        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());

        // 2. Try to register the same user again
        ResponseEntity<String> duplicateResponse = restClient.post()
                .uri("/api/v1/auth/register")
                .body(request)
                .exchange((req, res) -> {
                    // Capture the status and body without throwing
                    String body = res.bodyTo(String.class);
                    return ResponseEntity.status(res.getStatusCode()).body(body);
                });

        // 3. ✅ Assert 400 BAD_REQUEST (or 409 CONFLICT)
        assertEquals(HttpStatus.CONFLICT, duplicateResponse.getStatusCode());
        assertTrue(duplicateResponse.getBody().contains("Username already exists"));
    }

    @Test
    @DisplayName("Test login fail & return 401")
    public void login_fail_UserTest() {
        var request = new AuthController.RegisterRequest("testuser", "Asdfan3@#fadfa", "RETAIL_USER");

        restClient.post()
                .uri("/api/v1/auth/register")
                .body(request)
                .retrieve()
                .toEntity(AuthController.RegisterResponse.class);

        //Login
        var loginRequest = new AuthController.LoginRequest("testuser", "aAsdfan3@#fadfa");

        ResponseEntity<AuthController.LoginResponse> response = restClient.post()
                .uri("/api/v1/auth/login")
                .body(loginRequest)
                .exchange((req, res) -> ResponseEntity.status(res.getStatusCode()).build());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    }

    @Test
    @DisplayName("Test login succes + token validation")
    public void login_success_UserTest() {
//         Register first
        var request = new AuthController.RegisterRequest("testuser1", "secreASDf23$@#te", "RETAIL_USER");

        restClient.post()
                .uri("/api/v1/auth/register")
                .body(request)
                .retrieve()
                .toEntity(AuthController.RegisterResponse.class);


        // Login
        var loginRequest = new AuthController.LoginRequest("testuser1", "secreASDf23$@#te");
        var response = restClient.post()
                .uri("/api/v1/auth/login")
                .body(loginRequest)
                .retrieve()
                .toEntity(AuthController.LoginResponse.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertNotNull(response.getBody().token(), "Response token should not be null");
    }

    @Test
    @DisplayName("Test the rate limiting")
    public void check_rate_limiting() {
        var login = new AuthController.LoginRequest("Nonexist", "wrongad@#1123ADf");

        //1 First 5 request consume bucket token (400 error for not user not found in database)
        for (int i = 0; i < 5; i++) {
            var statusCode = restClient.post()
                    .uri("/api/v1/auth/login")
                    .body(login)
                    .exchange((req, res) -> res.getStatusCode());

            assertEquals(HttpStatus.UNAUTHORIZED, statusCode);
        }

        //2. Should return 429 error, bucket token exhausts
        var response = restClient.post()
                .uri("/api/v1/auth/login")
                .body(login)
                .exchange((req, res) -> res.getStatusCode());

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response);
    }
}