package com.bank.banking_api.controller;

import com.bank.banking_api.domain.Money;
import com.bank.banking_api.domain.Transaction;
import com.bank.banking_api.domain.TransactionStatus;
import com.bank.banking_api.persistence.JdbcTransactionRepository;
import com.bank.banking_api.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final JdbcTransactionRepository transactionRepository;

    public TransactionController(JdbcTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getTransactionHistory(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();

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

        return ResponseEntity.ok(dtos);
    }

    public record TransactionDto(
            UUID transactionId,
            String fromAccount,
            String toAccount,
            Money amount,
            TransactionStatus status,
            Instant createdAt,
            Instant completedAt
    ) {}
}