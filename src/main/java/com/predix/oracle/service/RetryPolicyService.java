package com.predix.oracle.service;

import com.predix.oracle.config.PredixOracleProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RetryPolicyService {

    private final PredixOracleProperties properties;

    public Instant nextRetryAt(int retryCount) {
        long delay = (long) (properties.getRetry().getBaseDelayMs()
                * Math.pow(properties.getRetry().getMultiplier(), retryCount));
        return Instant.now().plusMillis(delay);
    }

    public boolean canRetry(int retryCount) {
        return retryCount < properties.getRetry().getMaxAttempts();
    }

    public int maxAttempts() {
        return properties.getRetry().getMaxAttempts();
    }
}
