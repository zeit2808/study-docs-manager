package com.studydocs.manager.security.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static class Bucket {
        private int tokens;
        private final int capacity;
        private final long refillIntervalMillis;
        private long lastRefillTimestamp;

        Bucket(int capacity, long refillIntervalMillis) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillIntervalMillis = refillIntervalMillis;
            this.lastRefillTimestamp = Instant.now().toEpochMilli();
        }

        synchronized boolean tryConsume() {
            refillIfNeeded();

            if (tokens <= 0) {
                return false;
            }

            tokens--;
            return true;
        }

        private void refillIfNeeded() {
            long now = Instant.now().toEpochMilli();

            if (now - lastRefillTimestamp >= refillIntervalMillis) {
                tokens = capacity;
                lastRefillTimestamp = now;
            }
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int capacityPerMinute) {
        return tryConsume(key, capacityPerMinute, Duration.ofMinutes(1));
    }

    public boolean tryConsume(String key, int capacity, Duration interval) {
        Bucket bucket = buckets.computeIfAbsent(
                key,
                ignored -> new Bucket(capacity, interval.toMillis())
        );

        return bucket.tryConsume();
    }
}