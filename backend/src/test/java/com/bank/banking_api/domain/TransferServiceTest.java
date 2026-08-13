package com.bank.banking_api.domain;

import com.bank.banking_api.exception.AccessDeniedException;
import com.bank.banking_api.exception.InsufficientFundsException;
import com.bank.banking_api.persistence.JdbcTransactionRepository;
import com.bank.banking_api.security.CustomUserDetails;
import com.bank.banking_api.service.MetricsService;
import com.bank.banking_api.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private JdbcTransactionRepository jdbcTransactionRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private TransferService transferService;

    private final Currency currency = Currency.getInstance("USD");
    private final UUID currentUser = UUID.randomUUID();
    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        authenticateUser(currentUser);

        sourceAccount = mock(Account.class);
        targetAccount = mock(Account.class);

        lenient().when(metricsService.recordTransferDuration(any(Supplier.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<Object> supplier = invocation.getArgument(0);
            return supplier.get();
        });
    }

    @Test
    @DisplayName("Successful transfer should debit from source and credit the target")
    void successfulTransferCheck() {
        lenient().when(sourceAccount.getAccountNumber()).thenReturn("ACC-1");
        lenient().when(targetAccount.getAccountNumber()).thenReturn("ACC-2");
        lenient().when(sourceAccount.getOwnerId()).thenReturn(currentUser);
        lenient().when(targetAccount.getOwnerId()).thenReturn(currentUser);

        String used_key = UUID.randomUUID().toString();
        Money amount = Money.of(new BigDecimal("100.00"), currency);

        when(accountRepository.findByAccountNumberForUpdate("ACC-1")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberForUpdate("ACC-2")).thenReturn(Optional.of(targetAccount));
        when(jdbcTransactionRepository.findByIdempotencyKey(used_key)).thenReturn(Optional.empty());

        Transaction res = transferService.transfer("ACC-1", "ACC-2", amount, used_key, currentUser);

        assertNotNull(res);
        assertEquals(TransactionStatus.COMMITTED, res.getStatus());

        verify(sourceAccount, times(1)).debit(amount);
        verify(targetAccount, times(1)).credit(amount);
        verify(jdbcTransactionRepository, times(1)).save(any(Transaction.class));
        verify(metricsService, times(1)).incrementTransactionSuccessCounter();
    }


    // ==========================================
    //  TEST 2: Unauthorized User
    // ==========================================
    @Test
    @DisplayName("Unauthorized user should throw AccessDeniedException")
    void unauthorizedUser_shouldThrowAccessDenied() {
        Account othersAccount = mock(Account.class);
        UUID otherUser = UUID.randomUUID();

        lenient().when(othersAccount.getAccountNumber()).thenReturn("ACC-1");
        lenient().when(targetAccount.getAccountNumber()).thenReturn("ACC-2");
        lenient().when(othersAccount.getOwnerId()).thenReturn(otherUser);
        lenient().when(targetAccount.getOwnerId()).thenReturn(currentUser);

        // ✅ 1. Stub ONLY what this test needs

        String key = UUID.randomUUID().toString();
        Money amount = Money.of(new BigDecimal("100.00"), currency);

        when(accountRepository.findByAccountNumberForUpdate("ACC-1"))
                .thenReturn(Optional.of(othersAccount));
        when(accountRepository.findByAccountNumberForUpdate("ACC-2"))
                .thenReturn(Optional.of(targetAccount));
        when(jdbcTransactionRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.empty());

        // ✅ 2. Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            transferService.transfer("ACC-1", "ACC-2", amount, key, currentUser);
        });

        // ✅ 3. Verify
        verify(othersAccount, never()).debit(any());
        verify(targetAccount, never()).credit(any());
        verify(accountRepository, never()).update(any());
        verify(metricsService, times(1)).incrementTransactionFailureCounter("ACCESS_DENIED");
    }

    @Test
    @DisplayName("Same account transfer should throw error")
    void sameAccountTransfer_shouldThrowError() {
//        lenient().when(sourceAccount.getAccountNumber()).thenReturn("ACC-1");
//        lenient().when(sourceAccount.getOwnerId()).thenReturn(currentUser);

        String used_key = UUID.randomUUID().toString();
        Money amount = Money.of(new BigDecimal("100.00"), currency);

        assertThrows(IllegalArgumentException.class, () -> {
            transferService.transfer("ACC-1", "ACC-1", amount, used_key, currentUser);
        });

        verify(accountRepository, never()).update(any());
    }

    @Test
    @DisplayName("Insufficient funds should rollback")
    void insufficientFunds_shouldRollback() {
        lenient().when(sourceAccount.getAccountNumber()).thenReturn("ACC-1");
        lenient().when(targetAccount.getAccountNumber()).thenReturn("ACC-2");
        lenient().when(sourceAccount.getOwnerId()).thenReturn(currentUser);
        lenient().when(targetAccount.getOwnerId()).thenReturn(currentUser);

        String key = UUID.randomUUID().toString();
        Money amount = Money.of(new BigDecimal("9999.00"), currency);

        doThrow(new InsufficientFundsException("Insufficient funds"))
                .when(sourceAccount).debit(amount);

        when(accountRepository.findByAccountNumberForUpdate("ACC-1")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberForUpdate("ACC-2")).thenReturn(Optional.of(targetAccount));
        when(jdbcTransactionRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());

        assertThrows(InsufficientFundsException.class, () -> {
            transferService.transfer("ACC-1", "ACC-2", amount, key, currentUser);
        });

        verify(accountRepository, never()).update(any());
        verify(jdbcTransactionRepository, never()).save(any(Transaction.class));
        verify(metricsService, times(1)).incrementTransactionFailureCounter("INSUFFICIENT_FUNDS");
    }


    @Test
    @DisplayName("Duplicate idempotency key returns cached transaction")
    void idempotency_duplicateKey_returnsCached() {
        // ✅ 1. Stub ONLY what this test needs
        String key = "duplicate-key";
        Money amount = Money.of(new BigDecimal("100.00"), currency);

        Transaction existingTx = mock(Transaction.class);
        lenient().when(existingTx.getStatus()).thenReturn(TransactionStatus.COMMITTED);
        lenient().when(existingTx.getFromAccountId()).thenReturn("ACC-1");
        lenient().when(existingTx.getToAccountId()).thenReturn("ACC-2");
        lenient().when(existingTx.getAmount()).thenReturn(amount);
        lenient().when(existingTx.getId()).thenReturn(UUID.randomUUID());

        when(jdbcTransactionRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.of(existingTx));

        // ✅ 2. Act
        Transaction result = transferService.transfer("ACC-1", "ACC-2", amount, key, currentUser);

        // ✅ 3. Assert
        assertNotNull(result);
        assertEquals(existingTx.getId(), result.getId());

        // ✅ 4. Verify
        verify(jdbcTransactionRepository, never()).save(any(Transaction.class));
        verify(accountRepository, never()).findByAccountNumberForUpdate(anyString());
        verify(metricsService, times(1)).incrementIdempotencyHitCounter();
    }


    // Helper: Authenticate User for SecurityContext
    private void authenticateUser(UUID userId) {
        User user = new User(userId, "testuser", "hashed", AccountRole.RETAIL_USER, Instant.now());
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

//
//    @Test
//    @DisplayName("Should be perform concurreny transfer properly")
//    void testConcurrentTransfersDeadlock() throws Exception {
//        int thread = 2;
//
//        ExecutorService executor = Executors.newFixedThreadPool(thread);
//        CountDownLatch latch = new CountDownLatch(thread);
//
//        // Two thread: one transfer test-1 -> test-2, the other test-2 -> test-1
//        Runnable task1 = () -> {
//            try {
//                transferService.transfer("test-1", "test-2", Money.of(new BigDecimal("100.00"), INR), UUID.randomUUID().toString());
//            } finally {
//                latch.countDown();
//            }
//        };
//
//        Runnable task2 = () -> {
//            try {
//                transferService.transfer("test-2", "test-1", Money.of(new BigDecimal("50.00"), INR), UUID.randomUUID().toString());
//            } finally {
//                latch.countDown();
//            }
//        };
//
//        executor.submit(task1);
//        executor.submit(task2);
//        latch.await(10, TimeUnit.SECONDS);
//
//        //Final balance must be consistent total=1000+500=1500
//        Account acc1 = accountService.getAccount("test-1");
//        Account acc2 = accountService.getAccount("test-2");
//
//        BigDecimal total = acc1.getBalance().getAmount().add(acc2.getBalance().getAmount());
//
//        assertEquals(new BigDecimal("1500.00"), total);
//        executor.shutdown();
//    }