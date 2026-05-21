package com.predix.oracle.crawler;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class EvidenceAggregationResult {
    boolean quorumReached;
    String winningOutcome;
    BigDecimal agreementRatio;
    boolean requiresManualReview;
    List<String> distinctOutcomes;
}
