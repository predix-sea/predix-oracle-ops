package com.predix.oracle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private static final String LOCK_PREFIX = "predix:oracle:lock:";

    private final StringRedisTemplate redisTemplate;

    public boolean tryWithLock(String lockKey, Duration ttl, Runnable action) {
        String token = UUID.randomUUID().toString();
        String key = LOCK_PREFIX + lockKey;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        if (!Boolean.TRUE.equals(acquired)) {
            return false;
        }
        try {
            action.run();
            return true;
        } finally {
            String current = redisTemplate.opsForValue().get(key);
            if (token.equals(current)) {
                redisTemplate.delete(key);
            }
        }
    }

    public <T> T executeWithLock(String lockKey, Duration ttl, Supplier<T> supplier, T fallback) {
        String token = UUID.randomUUID().toString();
        String key = LOCK_PREFIX + lockKey;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("Lock not acquired: {}", lockKey);
            return fallback;
        }
        try {
            return supplier.get();
        } finally {
            String current = redisTemplate.opsForValue().get(key);
            if (token.equals(current)) {
                redisTemplate.delete(key);
            }
        }
    }
}
