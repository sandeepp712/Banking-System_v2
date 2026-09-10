package com.bank.banking_api.controller;

import com.bank.banking_api.domain.Money;
import com.bank.banking_api.domain.Transaction;
import com.bank.banking_api.domain.TransactionStatus;
import com.bank.banking_api.dto.TransactionDto;
import com.bank.banking_api.persistence.JdbcTransactionRepository;
import com.bank.banking_api.security.CustomUserDetails;
import com.bank.banking_api.service.TransferService;
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

    private final TransferService transferService;

    public TransactionController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getTransactionHistory(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();

        List<TransactionDto> dtos = transferService.getTransactionHistory(userId);
        return ResponseEntity.ok(dtos);
    }


}