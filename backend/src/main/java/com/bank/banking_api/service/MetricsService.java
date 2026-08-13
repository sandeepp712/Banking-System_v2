package com.bank.banking_api.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class MetricsService {
    private final MeterRegistry meterRegistry;
    private final Counter transactionSuccessCounter;
    private final Counter idempotencyHitCounter;
    private final Timer transferTimer;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.transactionSuccessCounter = Counter.builder("bank.transfers.success")
                .description("Total number of transactions sent successfully")
                .register(meterRegistry);

        this.idempotencyHitCounter = Counter.builder("bank.transfer.idempotency_hit")
                .description("Duplicate requests blocked by idmepotency key")
                .register(meterRegistry);

        this.transferTimer = Timer.builder("banking.transfer.duration")
                .description("Duration of money transfer operations")
                .register(meterRegistry);
    }

    public void incrementTransactionSuccessCounter() {
        transactionSuccessCounter.increment();
    }

    public void incrementIdempotencyHitCounter() {
        idempotencyHitCounter.increment();
    }

    public <T> T recordTransferDuration(Supplier<T> supplier) {
        return transferTimer.record(supplier);
    }

    /**
     * Dynamically registers/increments counters tagged by specific error codes.
     */
    public void incrementTransactionFailureCounter(String errorCode) {
        String safeErrorCode = (errorCode != null && !errorCode.isBlank()) ? errorCode : "UNKNOWN_ERROR";

        Counter.builder("bank.transfer.failure")
                .description("Total number of transactions sent failure")
                .tag("errorCode", safeErrorCode)
                .register(meterRegistry)
                .increment();
    }
}