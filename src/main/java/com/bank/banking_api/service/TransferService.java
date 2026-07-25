package com.bank.banking_api.service;

import com.bank.banking_api.domain.*;
import com.bank.banking_api.dto.TransactionResponse;
import com.bank.banking_api.exception.DuplicateTransactionException;
import com.bank.banking_api.persistence.JdbcTransactionRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;


@Service
public class TransferService {
    private final AccountRepository accountRepository;
    private final JdbcTransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    public TransferService(AccountRepository accountRepository, JdbcTransactionRepository transactionRepository, ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Transaction transfer(String fromAccountId, String toAccountId, Money amount, String idempotencyKey) {

        // 0. Validate input
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }


        //1. Check idempotency from database
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
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

        //2 Validate
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("From account id cannot be the same as to account id");
        }


        // Deadlock prevention: lock accounts in a global order (by account number)
        String firstLock = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
        String secondLock = firstLock.equals(fromAccountId) ? toAccountId : fromAccountId;

        // Acquire pessimistic locks (both inside the same transaction)
        Account first = accountRepository.findByAccountNumberForUpdate(firstLock).orElseThrow(() -> new IllegalArgumentException("Account not found" + firstLock));
        Account second = accountRepository.findByAccountNumberForUpdate(secondLock).orElseThrow(() -> new IllegalArgumentException("Account not found" + secondLock));

        // Map locked Accounts to actual from/to
        Account from = first.getAccountNumber().equals(fromAccountId) ? first : second;
        Account to = (from == first) ? second : first;

        //Business logic(Perform money transfer)
        from.debit(amount); //subtract from source
        to.credit(amount);   //add to destination

        // persist the changes - updates are done inside the transaction, locks held until commit
        accountRepository.update(from);
        accountRepository.update(to);

        Transaction committed = Transaction.builder().transactionId(UUID.randomUUID()).type(TransactionType.TRANSFER).fromAccountId(fromAccountId).toAccountId(toAccountId).Amount(amount).status(TransactionStatus.COMMITTED).idempotencyKey(idempotencyKey).responseCache("Success").errorMessage("none").createdAt(Instant.now()).completedAt(Instant.now()).build();

        try {
            transactionRepository.save(committed);
            return committed;
        } catch (DuplicateKeyException e) {
            return transactionRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> new IllegalStateException("Concurrent transaction failed unexceptedly"));
        }
    }
}