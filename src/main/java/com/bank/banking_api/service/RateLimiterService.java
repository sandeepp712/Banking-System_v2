package com.bank.banking_api.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    // Limit: 5 requests per minute per client (IP)
    private static final int CAPACITY = 5;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    // Store buckets per client key (IP address)
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String clientKey) {
        return cache.computeIfAbsent(clientKey, this::createNewBucket);
    }

    private Bucket createNewBucket(String clientKey) {
        Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.greedy(CAPACITY, REFILL_PERIOD));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Attemps to consume a token. Return true if allowed, false if rate limited.
     */
    public boolean tryConsume(String clientKey) {
        Bucket bucket = resolveBucket(clientKey);
        return bucket.tryConsume(1);
    }


    public void reset() {
        cache.clear();
    }
}