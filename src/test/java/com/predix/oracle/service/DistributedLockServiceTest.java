package com.predix.oracle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private DistributedLockService lockService;

    @Test
    void runsActionWhenLockAcquired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(valueOps.get(anyString())).thenReturn("token-mock-fail"); // skip delete path safely

        AtomicBoolean ran = new AtomicBoolean(false);
        boolean ok = lockService.tryWithLock("test-lock", Duration.ofSeconds(5), () -> ran.set(true));

        assertThat(ok).isTrue();
        assertThat(ran).isTrue();
    }

    @Test
    void skipsWhenLockNotAcquired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        AtomicBoolean ran = new AtomicBoolean(false);
        boolean ok = lockService.tryWithLock("test-lock", Duration.ofSeconds(5), () -> ran.set(true));

        assertThat(ok).isFalse();
        assertThat(ran).isFalse();
    }
}
