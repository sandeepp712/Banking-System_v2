package com.bank.banking_api.integration;

import com.bank.banking_api.domain.*;
import com.bank.banking_api.service.AccountService;
import com.bank.banking_api.service.TransferService;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransferServiceIntegrationTest {
    @LocalServerPort
    private int port;

    private RestClient restClient;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Currency INR = Currency.getInstance("INR");

    @BeforeEach
    public void setUp() {
        restClient = RestClient.create("http://localhost:" + port);

        jdbcTemplate.execute("TRUNCATE TABLE transactions RESTART IDENTITY CASCADE ");
        jdbcTemplate.execute("TRUNCATE TABLE accounts RESTART IDENTITY CASCADE ");

        accountService.createAccount("test-1", Money.of(new BigDecimal("1000.00"), INR));
        accountService.createAccount("test-2", Money.of(new BigDecimal("500.00"), INR));
    }


    @Test
    @DisplayName("Should transfer money from test-1 to test-2")
    void transferMoneyFromAToB() {
        transferService.transfer("test-1", "test-2", Money.of(new BigDecimal("100.00"), INR), UUID.randomUUID().toString());

        Account acc1 = accountService.getAccount("test-1");
        Account acc2 = accountService.getAccount("test-2");

        assertEquals(new BigDecimal("900.00"), acc1.getBalance().getAmount());
        assertEquals(new BigDecimal("600.00"), acc2.getBalance().getAmount());
    }

    @Test
    @DisplayName("Insufficient funds should rollback the transaction")
    void testRollbackOnInsufficientFunds() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer("test-2", "test-1", Money.of(new BigDecimal("1000.00"), INR), UUID.randomUUID().toString())
        );

        Account acc1 = accountService.getAccount("test-1");
        Account acc2 = accountService.getAccount("test-2");

        // Balances unchanged
        assertEquals(new BigDecimal("1000.00"), acc1.getBalance().getAmount());
        assertEquals(new BigDecimal("500.00"), acc2.getBalance().getAmount());
    }

    @Test
    @DisplayName("Should throw error insufficient funds")
    void transferMoneyFromInsufficientFunds() {
        assertThrows(IllegalArgumentException.class, () -> {
            transferService.transfer("test-1", "test-2", Money.of(new BigDecimal("1001.00"), INR), UUID.randomUUID().toString());
        });
    }

    @Test
    @DisplayName("Should be perform concurreny transfer properly")
    void testConcurrencyTransfer() throws Exception {
        int thread = 2;

        ExecutorService executor = Executors.newFixedThreadPool(thread);
        CountDownLatch latch = new CountDownLatch(thread);

        Runnable runnable_1 = () -> {
            try {
                transferService.transfer("test-1", "test-2", Money.of(new BigDecimal("10.00"), INR), UUID.randomUUID().toString());
            } finally {
                latch.countDown();
            }
        };

        Runnable runnable_2 = () -> {
            try {
                transferService.transfer("test-2", "test-1", Money.of(new BigDecimal("50.00"), INR), UUID.randomUUID().toString());
            } finally {
                latch.countDown();
            }
        };

        executor.execute(runnable_1);
        executor.execute(runnable_2);
        latch.await(10, TimeUnit.SECONDS);

        Account acc1 = accountService.getAccount("test-1");
        Account acc2 = accountService.getAccount("test-2");

        BigDecimal total = acc1.getBalance().getAmount().add(acc2.getBalance().getAmount());
        assertEquals(new BigDecimal("1500.00"), total);
        executor.shutdown();
    }

        @Test
        @DisplayName("Check the idempotency key stop the same transaction")
        void checkIdempotencyKey() {
            String key = UUID.randomUUID().toString();
            Money amount = Money.of(new BigDecimal("100.00"), INR);

            //1 Fist transfer: test-1 to test-2
            Transaction tx = transferService.transfer("test-1", "test-2", amount, key);
            assertEquals(TransactionStatus.COMMITTED, tx.getStatus());

            //2 Duplicate transaction with same key
            Transaction tx2 = transferService.transfer("test-1", "test-2", amount, key);
            assertEquals(tx.getId(),tx2.getId());


            Account acc1 = accountService.getAccount("test-1");
            Account acc2 = accountService.getAccount("test-2");

            assertEquals(new BigDecimal("900.00"), acc1.getBalance().getAmount());
            assertEquals(new BigDecimal("600.00"), acc2.getBalance().getAmount());
        }

    @Test
    @DisplayName("Duplicate request with different payload should throw exception")
    void duplicateRequestWithDifferentPayload() {
        String key = UUID.randomUUID().toString();
        Money amount100 = Money.of(new BigDecimal("100.00"), INR);
        Money amount200 = Money.of(new BigDecimal("200.00"), INR);

        // First request: 100
        transferService.transfer("test-2", "test-1", amount100, key);

        // Second request: same key, different amount -> should be rejected
        assertThrows(RuntimeException.class, () -> {
            transferService.transfer("test-2", "test-1", amount200, key);
        });
    }
}
