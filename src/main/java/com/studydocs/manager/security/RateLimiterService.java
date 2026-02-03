// RateLimiterService.java
package com.studydocs.manager.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static class Bucket {
        int tokens;
        int capacity;
        long refillIntervalMillis;
        long lastRefillTimestamp;

        Bucket(int capacity, long refillIntervalMillis) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillIntervalMillis = refillIntervalMillis;
            this.lastRefillTimestamp = Instant.now().toEpochMilli();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = Instant.now().toEpochMilli();
            long elapsed = now - lastRefillTimestamp;
            if (elapsed > refillIntervalMillis) {
                tokens = capacity;
                lastRefillTimestamp = now;
            }
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int capacityPerMinute) {
        long interval = 60_000L;
        Bucket bucket = buckets.computeIfAbsent(key,
                k -> new Bucket(capacityPerMinute, interval));
        return bucket.tryConsume();
    }
}