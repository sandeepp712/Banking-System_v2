package com.bank.banking_api.service;

import com.bank.banking_api.annotation.Auditable;
import com.bank.banking_api.domain.*;
import com.bank.banking_api.persistence.JdbcTransactionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service              // spring manage this bean(object)
public class AccountService {
    private final AccountRepository accountRepository;
    private final JdbcTransactionRepository transactionRepository;


    //Spring will automatically inject the JdbcAccountRepository here!
    public AccountService(AccountRepository accountRepository, JdbcTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
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

        System.out.println("DEBUG: JWT User ID = '" + currentUser + "'");
        System.out.println("DEBUG: DB Owner ID = '" + account.getOwnerId() + "'");

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
    @Auditable(action = "DEPOSIT")
    @Transactional
    public Account deposit(String accountNumber, Money amount, String idempotency_key, UUID currentUser) {

        //1 Check ownership First
        Account account = getAccount(accountNumber, currentUser);

        // Check the idempotency key
        Optional<Transaction> existingKey=transactionRepository.findByIdempotencyKey(idempotency_key);
        if (existingKey.isPresent()) {
            return  account;
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

        return account;
    }

    @Auditable(action = "WITHDRAW")
    @Transactional
    public Account withdraw(String accountNumber, Money amount, String idempotency_key, UUID currentUser) {
        //1 Check ownership FIRST
        Account account = getAccount(accountNumber, currentUser);

        // Check the idempotency key
        Optional<Transaction> existingKey=transactionRepository.findByIdempotencyKey(idempotency_key);
        if (existingKey.isPresent()) {
            return  account;
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

        return account;
    }

    public List<Account> getAccountsForUser(UUID currentUser) {
        return accountRepository.findAccountsForUser(currentUser);
    }
}