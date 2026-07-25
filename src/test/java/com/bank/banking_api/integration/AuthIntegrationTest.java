package com.bank.banking_api.integration;

import com.bank.banking_api.controller.AuthController;
import com.bank.banking_api.persistence.UserRepository;
import com.bank.banking_api.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthIntegrationTest {
    @LocalServerPort
    private int port;

    private RestClient restClient;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @BeforeEach
    public void setUp() {
        restClient = RestClient.create("http://localhost:" + port);
        rateLimiterService.reset();
        userRepository.findByUsername("testuser").ifPresent(user -> {
            userRepository.delete("testuser");
        });
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
    @DisplayName("Test login succes + token validation")
    public void login_success_UserTest() {
        // Register first
//        var request = new AuthController.RegisterRequest("testuser1", "secreASDf23$@#te", "RETAIL_USER");
//
//        restClient.post()
//                .uri("/api/v1/auth/register")
//                .body(request)
//                .retrieve()
//                .toEntity(AuthController.RegisterResponse.class);


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

            assertEquals(HttpStatus.BAD_REQUEST, statusCode);
        }

        //2. Should return 429 error, bucket token exhausts
        var response = restClient.post()
                .uri("/api/v1/auth/login")
                .body(login)
                .exchange((req, res) -> res.getStatusCode());

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response);
    }
}