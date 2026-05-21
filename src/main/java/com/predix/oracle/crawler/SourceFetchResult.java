package com.predix.oracle.crawler;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;

@Value
@Builder
public class SourceFetchResult {
    String sourceUrl;
    Map<String, Object> rawPayload;
    String normalizedOutcome;
    BigDecimal confidenceScore;
}
