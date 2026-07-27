package com.bank.banking_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

// This annotation tells spring: "watch all controllers for exceptions"
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    //1. Handle Domain speific Business Exceptions(400 Bad request)
    @ExceptionHandler({InsufficientFundsException.class, AccountFrozenException.class, CurrencyMismatchException.class,})
    public ResponseEntity<ErrorResponse> handleBusinessException(RuntimeException ex, HttpServletRequest request) {
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
    public ResponseEntity<ErrorResponse> handleAccountNotFoundException(RuntimeException ex, HttpServletRequest request) {
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
    public ResponseEntity<ErrorResponse> handleDuplicateKeyException(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "CONFLICT",
                "A request with this idempotency key is already being processed or has been completed.",
                HttpStatus.CONFLICT.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    //Handle Access Denied (security/Idor failure) -> 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "Access Denied.",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    //Handle Expired Token -> return 401
    @ExceptionHandler(JwtTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleJwtTokenExpiredException(JwtTokenExpiredException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "TOKEN_EXPIRED",
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // Handle Invalid Token -> return 401
    @ExceptionHandler(JwtTokenInvalidException.class)
    public ResponseEntity<ErrorResponse> handleJwtTokenInvalidException(JwtTokenInvalidException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_TOKEN",
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // Handle Invalid arguments (400 Bad requests)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
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
        log.error("Unexpected error occured at path: {}",request.getRequestURI(), ex);
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