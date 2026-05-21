package com.predix.oracle.crawler;

import com.predix.oracle.config.PredixOracleProperties;
import com.predix.oracle.repository.entity.OracleEvidenceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EvidenceAggregator {

    private final PredixOracleProperties properties;

    public EvidenceAggregationResult aggregate(List<OracleEvidenceEntity> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return EvidenceAggregationResult.builder()
                    .quorumReached(false)
                    .requiresManualReview(true)
                    .distinctOutcomes(List.of())
                    .agreementRatio(BigDecimal.ZERO)
                    .build();
        }

        Map<String, Long> counts = evidences.stream()
                .collect(Collectors.groupingBy(OracleEvidenceEntity::getNormalizedOutcomeCode, Collectors.counting()));

        long total = evidences.size();
        Map.Entry<String, Long> winner = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();

        BigDecimal ratio = BigDecimal.valueOf(winner.getValue())
                .divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP);

        boolean minSourcesMet = total >= properties.getEvidence().getMinSources();
        boolean quorum = ratio.compareTo(properties.getEvidence().getQuorumRatio()) >= 0;
        boolean tie = counts.values().stream().filter(v -> v.equals(winner.getValue())).count() > 1;

        return EvidenceAggregationResult.builder()
                .quorumReached(minSourcesMet && quorum && !tie)
                .winningOutcome(winner.getKey())
                .agreementRatio(ratio)
                .requiresManualReview(tie || !minSourcesMet || !quorum)
                .distinctOutcomes(new ArrayList<>(counts.keySet()))
                .build();
    }
}
