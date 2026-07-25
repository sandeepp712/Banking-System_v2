package com.bank.banking_api.service;

import com.bank.banking_api.domain.*;
import com.bank.banking_api.persistence.JdbcTransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public Account createAccount(String accountNo, Money initialBalance) {
        if (accountRepository.findByAccountNumber(accountNo).isPresent()) {
            throw new IllegalArgumentException("Account already exists : " + accountNo);
        }

        Account account = new Account(accountNo, initialBalance);
        accountRepository.save(account);

        return account;
    }


    /**
     * To get the particular account is present or not
     *
     * @param
     * @return
     */
    public Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new IllegalArgumentException("Account not found : " + accountNumber));
    }

    /**
     * Deposits money into an account.
     *
     * @return The Transaction record representing this deposit.
     */
    public Account deposit(String accountNumber, Money amount, String idempotency_key) {
        Account account = getAccount(accountNumber);
        account.credit(amount);
        accountRepository.update(account);
        Transaction transaction = Transaction.builder()
                .type(TransactionType.DEPOSIT)
                .fromAccountId(null)
                .toAccountId(accountNumber)
                .Amount(amount)
                .idempotencyKey(idempotency_key)
                .build();

        transactionRepository.save(transaction);

        return account;
    }


    public Account withdraw(String accountNumber, Money amount, String idempotency_key) {
        Account account = getAccount(accountNumber);
        account.debit(amount);
        accountRepository.update(account);
        Transaction transaction = Transaction.builder()
                .type(TransactionType.WITHDRAW)
                .fromAccountId(accountNumber)
                .toAccountId(null)
                .Amount(amount)
                .idempotencyKey(idempotency_key)
                .build();
        transactionRepository.save(transaction);

        return account;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();

    }
}