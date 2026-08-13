package com.bank.banking_api.controller;

import com.bank.banking_api.domain.Money;
import com.bank.banking_api.domain.Transaction;
import com.bank.banking_api.dto.TransactionResponse;
import com.bank.banking_api.dto.TransferRequest;
import com.bank.banking_api.security.CustomUserDetails;
import com.bank.banking_api.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static tools.jackson.databind.jsonFormatVisitors.JsonValueFormat.UUID;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    // Transfer from A to B
    @PostMapping
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest transferRequest,
                                                        @AuthenticationPrincipal CustomUserDetails currentUser) {

        // If client didn't send a key, generate one (fallback for simple clients)
        if (transferRequest.getIdempotencyKey() == null || transferRequest.getIdempotencyKey().isEmpty()) {
            throw new IllegalArgumentException("IDEMPOTENCY KEY CANNOT BE NULL or EMPTY");
        }

        Money amount = Money.of(transferRequest.amount(), Currency.getInstance("USD"));


        // Execute Transfer
        Transaction transaction = transferService.transfer(
                transferRequest.getFromAccountNumber(),
                transferRequest.getToAccountNumber(),
                amount,
                transferRequest.getIdempotencyKey(),
                currentUser.getUserId()
        );

        return ResponseEntity.ok(TransactionResponse.from(transaction));
    }
}