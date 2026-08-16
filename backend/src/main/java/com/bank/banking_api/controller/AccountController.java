package com.bank.banking_api.controller;

import com.bank.banking_api.domain.Account;
import com.bank.banking_api.domain.Money;
import com.bank.banking_api.dto.AccountDto;
import com.bank.banking_api.dto.CreateAccountRequest;
import com.bank.banking_api.dto.DepositRequest;
import com.bank.banking_api.dto.WithdrawRequest;
import com.bank.banking_api.security.CustomUserDetails;
import com.bank.banking_api.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Currency;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountService accountService;
    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }


    /**
     * GET /api/accounts/{accountNumber} - Securely get ONE account (with ownership verification)
     * <p>
     * Why this is secure:
     * - Uses @AuthenticationPrincipal to get the current user
     * - Passes user ID to service for ownership verification
     * - Returns 403 if user doesn't own the account (prevents IDOR)
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<Account> getAccount(@PathVariable String accountNumber, @AuthenticationPrincipal CustomUserDetails currentUser) {
        //Pass the user Id to the service
        Account account = accountService.getAccount(accountNumber, currentUser.getUserId());
        return ResponseEntity.ok(account);
    }


    /**
     * POST /api/accounts - Create a new account (with ownership)
     * <p>
     * Why this is secure:
     * - Uses request body (REST best practice)
     * - Assigns account to current user
     * - Prevents users from creating accounts for others
     */
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody CreateAccountRequest request, @AuthenticationPrincipal CustomUserDetails currentUser) {
        Money balance = Money.of(request.getAmount(), Currency.getInstance("USD"));
        Account account = accountService.createAccount(request.getAccountNumber(), balance, currentUser.getUserId());

        return ResponseEntity.ok(account);
    }

    /**
     * POST /api/accounts/{accountNumber}/deposit - Deposit money
     * <p>
     * Why this is secure:
     * - Passes user ID to service for ownership verification
     * - Uses request body for amount (not path)
     * - Generates idempotency key if not provided
     */
    //POST http://localhost:8080/api/accounts/ACC-999/deposit?amount=500
    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<Account> deposit(@PathVariable String accountNumber,
                                           @RequestBody DepositRequest request,
                                           @AuthenticationPrincipal CustomUserDetails currentUser) {
        // If client didn't send a key, generate one (fallback for simple clients)
//        String idempotency_key = request.getIdempotencyKey() != null
//                ? request.getIdempotencyKey()
//                : UUID.randomUUID().toString();


        Money money = Money.of(request.getAmount(), Currency.getInstance("USD"));

        Account account = accountService.deposit(
                accountNumber,
                money,
                request.getIdempotencyKey(),
                currentUser.getUserId()
        );

        return ResponseEntity.ok(account);
    }


    /**
     * POST /api/accounts/{accountNumber}/withdraw - Withdraw money
     * <p>
     * Why this is secure:
     * - Same ownership verification as deposit
     */
    // POST http://localhost:8080/api/accounts/ACC-999/withdraw?amount=200
    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<Account> withdraw(@PathVariable String accountNumber,
                                            @RequestBody WithdrawRequest request,
                                            @AuthenticationPrincipal CustomUserDetails currentUser) {


        // If client didn't send a key, generate one (fallback for simple clients)
//        String idempotency_key = request.getIdempotencyKey() != null
//                ? request.getIdempotencyKey()
//                : "WDR-" + UUID.randomUUID().toString();

        Money money = Money.of(request.getAmount(), Currency.getInstance("USD"));
        Account account = accountService.withdraw(
                accountNumber,
                money,
                request.getIdempotencyKey(),
                currentUser.getUserId()
        );
        return ResponseEntity.ok(account);
    }

    /**
     * GET /api/accounts/me - Get ALL accounts for the current user
     * <p>
     * Why this is secure:
     * - Only returns accounts owned by current user
     * - Prevents users from seeing others' accounts
     */
    //Get all accounts
    @GetMapping("/account")
    public ResponseEntity<List<AccountDto>> getMyAccounts(@AuthenticationPrincipal CustomUserDetails currentUser) {
//        log.info("CONTROLLER CHECK: currentUser={}", currentUser.getUserId());
        // Find accounts from DB
        List<Account> accounts = accountService.getAccountsForUser(currentUser.getUserId());

        List<AccountDto> accountDtos = accounts.stream()
                .map(account -> new AccountDto(
                        account.getAccountNumber(),
                        account.getBalance().getAmount(),
                        account.getBalance().getCurrency().getCurrencyCode()
                ))
                .toList();

        return ResponseEntity.ok(accountDtos);
    }
}