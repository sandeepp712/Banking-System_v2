package com.bank.banking_api.service;

import com.bank.banking_api.annotation.Auditable;
import com.bank.banking_api.domain.*;
import com.bank.banking_api.dto.TransactionDto;
import com.bank.banking_api.exception.AccountNotFoundException;
import com.bank.banking_api.exception.DuplicateTransactionException;
import com.bank.banking_api.exception.InsufficientFundsException;
import com.bank.banking_api.persistence.JdbcTransactionRepository;
import com.bank.banking_api.exception.AccessDeniedException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


@Service
public class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final String CACHE_KEY_PREFIX = "txn:recent:";
    private static final int BASE_TTL_SECONDS = 60;

    private final AccountRepository accountRepository;
    private final JdbcTransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;
    private final StringRedisTemplate redisTemplate;

    private final Counter cacheHitsCounter;
    private final Counter cacheMissesCounter;


    public TransferService(AccountRepository accountRepository, JdbcTransactionRepository transactionRepository, ObjectMapper objectMapper, MetricsService metricsService, StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
        this.redisTemplate = redisTemplate;

        this.cacheHitsCounter = Counter.builder("cache_hits_total")
                .tag("cache", "txn_recent")
                .description("Total cache hits for transaction history")
                .register(meterRegistry);

        this.cacheMissesCounter = Counter.builder("cache_misses_total")
                .tag("cache", "txn_recent")
                .description("Total cache misses for transaction history")
                .register(meterRegistry);
    }

    @Transactional
    @Auditable(action = "TRANSFER", sourceAccountArgIndex = 0, targetAccountArgIndex = 1)
    public Transaction transfer(String fromAccountId, String toAccountId, Money amount,
                                String idempotencyKey, UUID currentUser) {
        // 0. Validate input
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }

        //2 Validate
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("From account id cannot be the same as to account id");
        }

        return metricsService.recordTransferDuration(() -> {
            try {
                //1. Check idempotency from database
                Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    metricsService.incrementIdempotencyHitCounter();
                    Transaction tx = existing.get();

                    // 1. Payload Mismatch Check
                    boolean payloadMatches = tx.getFromAccountId().equals(fromAccountId)
                            && tx.getToAccountId().equals(toAccountId)
                            && tx.getAmount().equals(amount);

                    if (!payloadMatches) {
                        throw new IllegalArgumentException(
                                "Idempotency key '" + idempotencyKey + "' was previously used with a different request payload."
                        );
                    }

                    if (tx.getStatus() == TransactionStatus.COMMITTED) {
                        return tx;
                    } else if (tx.getStatus() == TransactionStatus.PENDING) {
                        throw new DuplicateTransactionException("Transaction is currently processing.");
                    } else if (tx.getStatus() == TransactionStatus.FAILED) {
                        throw new DuplicateTransactionException("Previous attempt failed. Please retry with new key.");
                    }
                }


                // Deadlock prevention: lock accounts in a global order (by account number)
                String firstLock = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
                String secondLock = firstLock.equals(fromAccountId) ? toAccountId : fromAccountId;

                // Acquire pessimistic locks (both inside the same transaction)
                Account first = accountRepository.findByAccountNumberForUpdate(firstLock).orElseThrow(() -> new IllegalArgumentException("Account not found " + firstLock));
                Account second = accountRepository.findByAccountNumberForUpdate(secondLock).orElseThrow(() -> new IllegalArgumentException("Account not found " + secondLock));


                // Map locked Accounts to actual from/to
                Account from = first.getAccountNumber().equals(fromAccountId) ? first : second;
                Account to = (from == first) ? second : first;

                // Critical security check: does the current user own the 'from' account?
                if (!from.getOwnerId().equals(currentUser)) {
                    throw new AccessDeniedException("You do not have permission to access.");
                }

                //Business logic(Perform money transfer)
                from.debit(amount); //subtract from source
                to.credit(amount);   //add to destination

                // persist the changes - updates are done inside the transaction, locks held until commit
                accountRepository.update(from);
                accountRepository.update(to);

                Transaction committed = Transaction.builder().transactionId(UUID.randomUUID()).type(TransactionType.TRANSFER).fromAccountId(fromAccountId).toAccountId(toAccountId).Amount(amount).status(TransactionStatus.COMMITTED).idempotencyKey(idempotencyKey).responseCache("Success").errorMessage("none").createdAt(Instant.now()).completedAt(Instant.now()).build();

                transactionRepository.save(committed);
                metricsService.incrementTransactionSuccessCounter();

                UUID senderUserId = from.getOwnerId();
                UUID receiverUserId = to.getOwnerId();

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            List<String> keysToDelete = List.of(
                                    "txn:recent:" + senderUserId,
                                    "txn:recent:" + receiverUserId
                            );
                            redisTemplate.delete(keysToDelete);
                        } catch (Exception e) {
                            log.warn("Post-commit cache eviction failed for users {} and {}:{}",
                                    senderUserId, receiverUserId, e.getMessage());
                        }
                    }
                });

                return committed;
            } catch (RuntimeException e) {
                String errorCode = resolveErrorCode(e);
                metricsService.incrementTransactionFailureCounter(errorCode);
                throw e;
            }

        });
    }


    public List<TransactionDto> getTransactionHistory(UUID userId) {
        String cacheKey = CACHE_KEY_PREFIX + userId.toString();

        try {
            String cacheJson=redisTemplate.opsForValue().get(cacheKey);
            if (cacheJson != null) {
                cacheHitsCounter.increment();
                return objectMapper.readValue(cacheJson, new TypeReference<List<TransactionDto>>() {});
            }
        }catch (Exception e){
            log.warn("Redis read failure for key {}. Falling back to DB: {}", cacheKey, e.getMessage());
        }

        cacheMissesCounter.increment();

        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        List<TransactionDto> dtos = transactions.stream()
                .map(tx -> new TransactionDto(
                        tx.getId(),
                        tx.getFromAccountId(),
                        tx.getToAccountId(),
                        tx.getAmount(),
                        tx.getStatus(),
                        tx.getCreatedAt(),
                        tx.getCompletedAt()
                ))
                .toList();

        try {
            String json=objectMapper.writeValueAsString(dtos);
            long jitterTtl= BASE_TTL_SECONDS + ThreadLocalRandom.current().nextInt(-6,7);
            redisTemplate.opsForValue().set(cacheKey,json, Duration.ofSeconds(jitterTtl));
        }catch (Exception e){
            log.warn("Redis write failure for key {}:{}",cacheKey,e.getMessage());
        }
        return dtos;
    }

    // Add this helper method to TransferService
    private String resolveErrorCode(RuntimeException e) {
        if (e instanceof InsufficientFundsException) return "INSUFFICIENT_FUNDS";
        if (e instanceof AccountNotFoundException) return "ACCOUNT_NOT_FOUND";
        if (e instanceof AccessDeniedException) return "ACCESS_DENIED";
        if (e instanceof DuplicateTransactionException) return "DUPLICATE_TRANSACTION";
        if (e instanceof IllegalArgumentException) return "INVALID_ARGUMENT";
        return "UNKNOWN_ERROR";
    }
}

