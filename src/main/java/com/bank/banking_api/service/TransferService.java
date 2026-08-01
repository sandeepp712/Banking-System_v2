package com.bank.banking_api.service;

import com.bank.banking_api.annotation.Auditable;
import com.bank.banking_api.domain.*;
import com.bank.banking_api.dto.TransactionResponse;
import com.bank.banking_api.exception.AccountNotFoundException;
import com.bank.banking_api.exception.DuplicateTransactionException;
import com.bank.banking_api.exception.InsufficientFundsException;
import com.bank.banking_api.persistence.JdbcTransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;


@Service
public class TransferService {
    private final AccountRepository accountRepository;
    private final JdbcTransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    public TransferService(AccountRepository accountRepository, JdbcTransactionRepository transactionRepository, ObjectMapper objectMapper, MetricsService metricsService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    @Transactional
    @Auditable(action = "TRANSFER",sourceAccountArgIndex = 0, targetAccountArgIndex = 1)
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

                return committed;
            } catch (RuntimeException e) {
                String errorCode=resolveErrorCode(e);
                metricsService.incrementTransactionFailureCounter(errorCode);
                throw e;
            }

        });
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

