package com.bank.banking_api.exception;

import java.time.Instant;

public record ErrorResponse(
        String errorCode,
        String message,
        int status,
        String path,
        Instant timestamp) {
}