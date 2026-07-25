package com.bank.banking_api.domain;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    void save(Account accounts);

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumberForUpdate(String accountNumber);

    void update(Account account);

    List<Account> findAll();

    void delete(String accountNumber);
}