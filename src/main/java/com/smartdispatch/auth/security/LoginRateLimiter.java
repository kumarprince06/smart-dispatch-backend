package com.smartdispatch.auth.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter to prevent brute-force login attacks.
 * Tracks failed login attempts per email and blocks after max attempts.
 * Resets on successful login.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    private final Map<String, AtomicInteger> attemptsCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lockCache = new ConcurrentHashMap<>();

    // Check if user is blocked
    public boolean isBlocked(String email) {
        Long lockTime = lockCache.get(email);
        if (lockTime == null) {
            return false;
        }

        if (System.currentTimeMillis() - lockTime > LOCK_DURATION_MS) {
            // Lock expired — reset
            lockCache.remove(email);
            attemptsCache.remove(email);
            return false;
        }

        return true;
    }

    // Record a failed login attempt
    public void recordFailedAttempt(String email) {
        AtomicInteger attempts = attemptsCache.computeIfAbsent(
                email, k -> new AtomicInteger(0)
        );

        if (attempts.incrementAndGet() >= MAX_ATTEMPTS) {
            lockCache.put(email, System.currentTimeMillis());
        }
    }

    // Reset on successful login
    public void resetAttempts(String email) {
        attemptsCache.remove(email);
        lockCache.remove(email);
    }

    // Get remaining attempts
    public int getRemainingAttempts(String email) {
        AtomicInteger attempts = attemptsCache.get(email);
        if (attempts == null) {
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - attempts.get());
    }
}
