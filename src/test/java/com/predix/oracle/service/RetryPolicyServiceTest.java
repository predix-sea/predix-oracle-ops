package com.predix.oracle.service;

import com.predix.oracle.config.PredixOracleProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyServiceTest {

    private RetryPolicyService retryPolicyService;

    @BeforeEach
    void setUp() {
        PredixOracleProperties props = new PredixOracleProperties();
        props.getRetry().setMaxAttempts(5);
        props.getRetry().setBaseDelayMs(1000L);
        props.getRetry().setMultiplier(2.0);
        retryPolicyService = new RetryPolicyService(props);
    }

    @Test
    void exponentialBackoffIncreases() {
        Instant first = retryPolicyService.nextRetryAt(0);
        Instant second = retryPolicyService.nextRetryAt(1);
        assertThat(second).isAfter(first);
    }

    @Test
    void canRetryWithinLimit() {
        assertThat(retryPolicyService.canRetry(4)).isTrue();
        assertThat(retryPolicyService.canRetry(5)).isFalse();
    }
}
