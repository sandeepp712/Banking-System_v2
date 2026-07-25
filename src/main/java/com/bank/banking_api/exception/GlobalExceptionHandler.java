package com.bank.banking_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

// This annotation tells spring: "watch all controllers for exceptions"
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = Logger.getLogger(GlobalExceptionHandler.class.getName());


    //1. Handle Domain speific Business Exceptions(400 Bad request)
    @ExceptionHandler({InsufficientFundsException.class, AccountFrozenException.class, CurrencyMismatchException.class,})
    public ResponseEntity<?> handleBusinessException(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                extractErrorCode(ex),
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    //2. Handle Missing Resources(404 Not found)
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<?> handleAccountNotFoundException(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "ACCOUNT_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    //3 Handle Idempotency
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> handleDuplicateKeyException(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "Duplicate_REQUEST",
                "A request with this idempotency key is already being processed or has been completed.",
                HttpStatus.CONFLICT.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 5. The Safety Net: Catch-All for Unexpected Errors (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        // LOG THE FULL STACK TRACE HERE for debugging
        log.log(Level.SEVERE, ex.getMessage(), ex);
        // NEVER expose the actual exception message or stack trace to the client
        ErrorResponse error = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Helper to extract clean error codes
    private String extractErrorCode(RuntimeException ex) {
        if (ex instanceof InsufficientFundsException) return "INSUFFICIENT_FUNDS";
        if (ex instanceof AccountFrozenException) return "ACCOUNT_FROZEN";
        if (ex instanceof CurrencyMismatchException) return "CURRENCY_MISMATCH";
        return "BUSINESS_ERROR";
    }


}