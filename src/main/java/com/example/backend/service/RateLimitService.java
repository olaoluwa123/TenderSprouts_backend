package com.example.backend.service;

import com.example.backend.config.SecurityProperties;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final SecurityProperties securityProperties;
    private final ConcurrentHashMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    public RateLimitService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public boolean tryConsumeAuth(String clientKey) {
        SecurityProperties.RateLimit limits = securityProperties.getRateLimit();
        return tryConsume(
            "auth:" + clientKey,
            limits.getAuthRequestsPerMinute(),
            Duration.ofMinutes(1),
            limits.isEnabled());
    }

    public boolean tryConsumeApi(String clientKey) {
        SecurityProperties.RateLimit limits = securityProperties.getRateLimit();
        return tryConsume(
            "api:" + clientKey,
            limits.getApiRequestsPerMinute(),
            Duration.ofMinutes(1),
            limits.isEnabled());
    }

    private boolean tryConsume(String key, int limit, Duration window, boolean enabled) {
        if (!enabled || limit <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();
        Deque<Long> timestamps = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= limit) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }
}
