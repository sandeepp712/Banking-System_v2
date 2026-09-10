package com.bank.banking_api.service;

import com.bank.banking_api.annotation.Auditable;
import com.bank.banking_api.domain.*;
import com.bank.banking_api.persistence.JdbcTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service              // spring manage this bean(object)
public class AccountService {
    private static final Logger log= LoggerFactory.getLogger(AccountService.class);
    private static final String CACHE_KEY_PREFIX = "txn:recent:";
    private static final int BASE_TTL_SECONDS = 60;

    private final AccountRepository accountRepository;
    private final JdbcTransactionRepository transactionRepository;
    private final StringRedisTemplate redisTemplate;



    //Spring will automatically inject the JdbcAccountRepository here!
    public AccountService(AccountRepository accountRepository, JdbcTransactionRepository transactionRepository, StringRedisTemplate redisTemplate) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Creates a new account and saves it to the repository.
     */
    public Account createAccount(String accountNo, Money initialBalance, UUID ownerId) {
        if (accountRepository.findByAccountNumber(accountNo).isPresent()) {
            throw new IllegalArgumentException("Account already exists : " + accountNo);
        }

        Account account = new Account(accountNo, initialBalance, ownerId);
        accountRepository.save(account);

        return account;
    }


    /**
     * To get the particular account is present or not
     *
     * @param
     * @return
     */
    public Account getAccount(String accountNumber, UUID currentUser) {
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new IllegalArgumentException("You do not have permission to access this account."));

        log.debug("Access check: userId={},ownerId={}", currentUser, account.getOwnerId());

        if (!account.getOwnerId().equals(currentUser)) {
            throw new AccessDeniedException("You do not have permission to access this account.");
        }
        return account;
    }

    /**
     * Deposits money into an account.
     *
     * @return The Transaction record representing this deposit.
     */
    @Auditable(action = "DEPOSIT", sourceAccountArgIndex = 0, targetAccountArgIndex = -1)
    @Transactional
    public Account deposit(String accountNumber, Money amount, String idempotency_key, UUID currentUser) {

        //1 Check ownership First
        Account account = getAccount(accountNumber, currentUser);

        // Check the idempotency key
        Optional<Transaction> existingKey = transactionRepository.findByIdempotencyKey(idempotency_key);
        if (existingKey.isPresent()) {
            Transaction tx = existingKey.get();

            boolean payloadMatches = accountNumber.equals(tx.getToAccountId())
                    && tx.getAmount().equals(amount);

            if(!payloadMatches) {
                throw new IllegalArgumentException("Idempotency key "+ idempotency_key + " was previously used with a differnet payload.");
            }

            if(tx.getStatus() == TransactionStatus.COMMITTED) {
                return account;
            }else if(tx.getStatus() == TransactionStatus.PENDING){
                throw new IllegalStateException("Transaction is currently processing");
            }else if(tx.getStatus() == TransactionStatus.FAILED){
                throw new IllegalStateException("Previous attempt failed. Please retry with a new key.");
            }
        }

        //2 Perform business logic
        account.credit(amount);
        accountRepository.update(account);

        //3 Record ledger
        Transaction transaction = Transaction.builder()
                .type(TransactionType.DEPOSIT)
                .fromAccountId(null)
                .toAccountId(accountNumber)
                .Amount(amount)
                .status(TransactionStatus.COMMITTED)
                .idempotencyKey(idempotency_key)
                .completedAt(Instant.now())
                .responseCache("Success")
                .errorMessage("none")
                .build();

        transactionRepository.save(transaction);
        UUID receiverId = account.getOwnerId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    redisTemplate.delete(List.of("txn:recent:" + receiverId));
                } catch (Exception e) {
                    log.warn("Post-commit cache eviction failed for user {}: {}", receiverId, e.getMessage());
                }
            }
        });

        return account;
    }

    @Auditable(action = "WITHDRAW", sourceAccountArgIndex = 0, targetAccountArgIndex = -1)
    @Transactional
    public Account withdraw(String accountNumber, Money amount, String idempotency_key, UUID currentUser) {
        //1 Check ownership FIRST
        Account account = getAccount(accountNumber, currentUser);

        // Check the idempotency key
        Optional<Transaction> existingKey = transactionRepository.findByIdempotencyKey(idempotency_key);
        if (existingKey.isPresent()) {
            Transaction tx = existingKey.get();

            boolean payloadMatches = accountNumber.equals(tx.getFromAccountId())
                    && tx.getAmount().equals(amount);

            if(!payloadMatches) {
                throw new IllegalArgumentException("Idempotency key "+ idempotency_key + " was previously used with a differnet payload.");
            }

            if(tx.getStatus() == TransactionStatus.COMMITTED) {
                return account;
            }else if(tx.getStatus() == TransactionStatus.PENDING){
                throw new IllegalStateException("Transaction is currently processing");
            }else if(tx.getStatus() == TransactionStatus.FAILED){
                throw new IllegalStateException("Previous attempt failed. Please retry with a new key.");
            }
        }

        //2 Perform business logic
        account.debit(amount);
        accountRepository.update(account);

        //3 Record ledger
        Transaction transaction = Transaction.builder()
                .type(TransactionType.WITHDRAW)
                .fromAccountId(accountNumber)
                .toAccountId(null)
                .Amount(amount)
                .status(TransactionStatus.COMMITTED)
                .idempotencyKey(idempotency_key)
                .completedAt(Instant.now())
                .responseCache("Success")
                .errorMessage("none")
                .build();
        transactionRepository.save(transaction);


        UUID senderId = account.getOwnerId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    redisTemplate.delete(List.of("txn:recent:" + senderId));
                } catch (Exception e) {
                    log.warn("Post-commit cache eviction failed for user {}: {}", senderId, e.getMessage());
                }
            }
        });

        return account;
    }

    public List<Account> getAccountsForUser(UUID currentUser) {
        return accountRepository.findAccountsForUser(currentUser);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAllAccounts();
    }
}