package com.bank.banking_api.integration;

import com.bank.banking_api.domain.*;
import com.bank.banking_api.exception.AccessDeniedException;
import com.bank.banking_api.exception.DuplicateTransactionException;
import com.bank.banking_api.exception.InsufficientFundsException;
import com.bank.banking_api.persistence.UserRepository;
import com.bank.banking_api.security.CurrentUserProvider;
import com.bank.banking_api.security.CustomUserDetails;
import com.bank.banking_api.service.AccountService;
import com.bank.banking_api.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class TransferServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AccountService accountService;
    @Autowired
    private TransferService transferService;
    @Autowired
    private UserRepository userRepository;

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
    void setUp() {
        // Wipe data in the correct order
        jdbcTemplate.execute("TRUNCATE TABLE transactions CASCADE ");
        jdbcTemplate.execute("TRUNCATE TABLE accounts CASCADE ");
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE ");

        // 1. Create a test user (since accounts have a foreign key to users)
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role) VALUES (?, ?, ?, ?)",
                userId, "testuser", "hashedpass#@23", "ROLE_USER"
        );

        UUID userId2 = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role) VALUES (?, ?, ?, ?)",
                userId2, "testuser1", "hashedpassas#da3", "ROLE_USER"
        );

        // 2. Insert accounts using the correct columns
        UUID acc1Id = UUID.randomUUID();
        UUID acc2Id = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO accounts (id, account_number, balance_amount, balance_currency, user_id) VALUES (?, ?, ?, ?, ?)",
                acc1Id, "ACC-1", new BigDecimal("1000.00"), "USD", userId
        );

        jdbcTemplate.update(
                "INSERT INTO accounts (id, account_number, balance_amount, balance_currency, user_id) VALUES (?, ?, ?, ?, ?)",
                acc2Id, "ACC-2", new BigDecimal("2000.00"), "USD", userId2
        );
    }

//   To check the db table
//    @Test
//    void inspectDatabase() throws Exception {
//        // 1. Print connection details
//        System.out.println("====== DB CONNECT DETAILS ======");
//        System.out.println("JDBC URL : " + postgres.getJdbcUrl());
//        System.out.println("Host Port: " + postgres.getFirstMappedPort());
//        System.out.println("Database : " + postgres.getDatabaseName());
//        System.out.println("Username : " + postgres.getUsername());
//        System.out.println("=================================");
//
//        Thread.sleep(360_000);
//    }

    @Test
    @DisplayName("Transfer amount from ACC-1 to ACC-2")
    void transferAmountFromAccountToAccount() {
        String key = UUID.randomUUID().toString();
        Money amount = Money.of(new BigDecimal("100.00"), Currency.getInstance("USD"));
        UUID currentUser = getTestUserId();
        authenticateUser(currentUser);

        Transaction tx = transferService.transfer("ACC-1", "ACC-2", amount, key, currentUser);

        assertNotNull(tx);
        assertEquals(TransactionStatus.COMMITTED, tx.getStatus());

        BigDecimal balance1 = jdbcTemplate.queryForObject("select balance_amount from accounts where account_number = 'ACC-1'", BigDecimal.class);
        BigDecimal balance2 = jdbcTemplate.queryForObject("select balance_amount from accounts where account_number = 'ACC-2'", BigDecimal.class);

        assertEquals(new BigDecimal("900.00"), balance1, "ACC-1 should be debited by 100");
        assertEquals(new BigDecimal("2100.00"), balance2, "ACC-2 should be credited by 100");
    }

    @Test
    @DisplayName("2. Idempotency: same key twice return cached transaction")
    void idempptency_same_key_return_cached_transaction() {
        String key = UUID.randomUUID().toString();
        Money amount = Money.of(new BigDecimal("100.00"), Currency.getInstance("USD"));
        UUID currentUser = getTestUserId();
        authenticateUser(currentUser);

        // First request
        Transaction tx1 = transferService.transfer("ACC-1", "ACC-2", amount, key, currentUser);
        assertNotNull(tx1);
        assertEquals(TransactionStatus.COMMITTED, tx1.getStatus());

        // Second request (DUPLICATE)
        Transaction tx2 = transferService.transfer("ACC-1", "ACC-2", amount, key, currentUser);

        //Must return the same transaction by ID
        assertEquals(tx1.getId(), tx2.getId(), "Should return the cached transaction ID");

        //Verify the balance changed only once
        BigDecimal balance1 = jdbcTemplate.queryForObject("select balance_amount from accounts where account_number = 'ACC-1'", BigDecimal.class);
        assertEquals(new BigDecimal("900.00"), balance1);
    }


    @Test
    @DisplayName("3.Idempotency: same key, different payload amounts should throw exception")
    void idempptency_different_key_return_cached_transaction() {
        String key = UUID.randomUUID().toString();
        UUID currentUser = getTestUserId();
        authenticateUser(currentUser);

        Money amount = Money.of(new BigDecimal("100.00"), Currency.getInstance("USD"));
        Money amount2 = Money.of(new BigDecimal("200.00"), Currency.getInstance("USD"));

        transferService.transfer("ACC-1", "ACC-2", amount, key, currentUser);

        assertThrows(IllegalArgumentException.class, () -> {
            transferService.transfer("ACC-1", "ACC-2", amount2, key, currentUser);
        }, "Should reject different payload with the same key");

        BigDecimal balance1 = jdbcTemplate.queryForObject("select balance_amount from accounts where account_number = 'ACC-1'", BigDecimal.class);
        BigDecimal balance2 = jdbcTemplate.queryForObject("select balance_amount from accounts where account_number = 'ACC-2'", BigDecimal.class);

        assertEquals(new BigDecimal("900.00"), balance1);
        assertEquals(new BigDecimal("2100.00"), balance2);
    }


    @Test
    @DisplayName("4. Insufficient Funds: Transfer fails, balances unchanged")
    void insufficientFunds_rollback() {
        String key = UUID.randomUUID().toString();
        Money amount = Money.of(new BigDecimal("9999.00"), Currency.getInstance("USD"));
        UUID currentUser = getTestUserId();

        // Try to transfer from ACC-2 (which has only 2000) to ACC-1
        assertThrows(InsufficientFundsException.class, () -> {
            transferService.transfer("ACC-1", "ACC-2", amount, key, currentUser);
        });

        // Verify balances are UNCHANGED (1000 and 2000)
        BigDecimal balance1 = jdbcTemplate.queryForObject(
                "SELECT balance_amount FROM accounts WHERE account_number = 'ACC-1'",
                BigDecimal.class
        );
        BigDecimal balance2 = jdbcTemplate.queryForObject(
                "SELECT balance_amount FROM accounts WHERE account_number = 'ACC-2'",
                BigDecimal.class
        );

        assertEquals(new BigDecimal("1000.00"), balance1);
        assertEquals(new BigDecimal("2000.00"), balance2);

        // 3. ✅ Verify NO transaction exists (rollback deleted everything)
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE idempotency_key = ?",
                Integer.class, key
        );
        assertEquals(0, count, "No transaction should exist after rollback");
    }


    @Test
    @DisplayName("5. Security: Transfer from someone else's account throws AccessDenied")
    void security_unauthorizedUser_throwsAccessDenied() {
        String key = UUID.randomUUID().toString();
        Money amount = Money.of(new BigDecimal("100.00"), Currency.getInstance("USD"));

        // Create a DIFFERENT user (not the one who owns ACC-1)
        UUID maliciousUser = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role) VALUES (?, ?, ?, ?)",
                maliciousUser, "hacker", "hashed", "ROLE_RETAIL"
        );

        // Try to transfer from ACC-1 (owned by original user) using the hacker's UUID
        assertThrows(AccessDeniedException.class, () -> {
            transferService.transfer("ACC-1", "ACC-2", amount, key, maliciousUser);
        }, "Should deny access to non-owner");
    }



    private UUID getTestUserId() {
        return jdbcTemplate.queryForObject("select id from users where username = 'testuser'", UUID.class);
    }

    private UUID getTestUserId2() {
        return jdbcTemplate.queryForObject("select id from users where username = 'testuser1'", UUID.class);
    }

    private void authenticateUser(UUID userId) {
        String username = jdbcTemplate.queryForObject("select username from users where id = ?", String.class, userId);

        User user = new User(userId, username, "hashed", AccountRole.RETAIL_USER, Instant.now());
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Authentication createAuthentication(UUID userId) {
        // Fetch user from DB or construct a minimal User object
        String username = jdbcTemplate.queryForObject("select username from users where id = ?", String.class, userId);

        User user = new User(userId, username, "hashed", AccountRole.RETAIL_USER, Instant.now());
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    @Test
    @DisplayName("6. Concurrency: 20 concurrent random transfers maintain total balance")
    void concurrentTransfers_moneyConservation() throws Exception {
        UUID ownerofAcc1 = getTestUserId();
        UUID ownerofAcc2 = getTestUserId2();

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Exception> error = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            final boolean acc1Toacc2 = (i % 2 == 0);

            executor.submit(() -> {
                try {
                    String key = UUID.randomUUID().toString(); // ✅ Fresh key per transfer
                    Money amount = Money.of(new BigDecimal("10.00"), Currency.getInstance("USD"));

                    if (acc1Toacc2) {
                        SecurityContextHolder.getContext().setAuthentication(createAuthentication(ownerofAcc1));
                        transferService.transfer("ACC-1", "ACC-2", amount, key, ownerofAcc1);
                    } else {
                        SecurityContextHolder.getContext().setAuthentication(createAuthentication(ownerofAcc2));
                        transferService.transfer("ACC-2", "ACC-1", amount, key, ownerofAcc2);
                    }
                } catch (InsufficientFundsException e) {
                    // ✅ Expected when an account runs low – ignore
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    latch.countDown();
                    // ✅ Clear context to avoid memory leaks
                    SecurityContextHolder.clearContext();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertNull(error.get(), "Concurrent execution threw an unexpected exception");

        // THE GOLDEN RULE: Total balance must ALWAYS equal 3000 (1000 + 2000)
        BigDecimal balance1 = jdbcTemplate.queryForObject(
                "SELECT balance_amount FROM accounts WHERE account_number = 'ACC-1'",
                BigDecimal.class
        );
        BigDecimal balance2 = jdbcTemplate.queryForObject(
                "SELECT balance_amount FROM accounts WHERE account_number = 'ACC-2'",
                BigDecimal.class
        );
        BigDecimal total = balance1.add(balance2);

        assertEquals(new BigDecimal("3000.00"), total, "Money conservation violated!");
    }


    @Test
    @DisplayName("7. Concurrency: Same idempotency key, 10 threads → exactly 1 commits")
    void concurrentSameIdempotencyKey_exactlyOneCommits() throws Exception {
        String sharedKey = UUID.randomUUID().toString();
        Money amount = Money.of(new BigDecimal("10.00"), Currency.getInstance("USD"));
        UUID owner = getTestUserId();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1); // Ensures all threads start simultaneously
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger cachedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // All threads wait here until released
                    Transaction tx = transferService.transfer("ACC-1", "ACC-2", amount, sharedKey, owner);
                    if (tx.getStatus() == TransactionStatus.COMMITTED) {
                        successCount.incrementAndGet();
                    }
                } catch (DuplicateTransactionException e) {
                    cachedCount.incrementAndGet();
                } catch (Exception e) {
                    // Unexpected
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown(); // Release all threads simultaneously
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly one thread should have created the transaction
        // The rest should have gotten the cached result or a duplicate exception
        Integer txCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE idempotency_key = ?", Integer.class, sharedKey);
        assertEquals(1, txCount, "Only ONE transaction record should exist");

        // Balance should reflect exactly ONE transfer of 10.00
        BigDecimal balance1 = jdbcTemplate.queryForObject(
                "SELECT balance_amount FROM accounts WHERE account_number = 'ACC-1'", BigDecimal.class);
        assertEquals(new BigDecimal("990.00"), balance1);
    }
}