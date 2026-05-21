package com.predix.oracle.crawler;

import com.predix.oracle.config.PredixOracleProperties;
import com.predix.oracle.repository.entity.OracleEvidenceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceAggregatorTest {

    private EvidenceAggregator aggregator;

    @BeforeEach
    void setUp() {
        PredixOracleProperties props = new PredixOracleProperties();
        props.getEvidence().setMinSources(2);
        props.getEvidence().setQuorumRatio(new BigDecimal("0.51"));
        aggregator = new EvidenceAggregator(props);
    }

    @Test
    void quorumReachedWhenMajorityAgrees() {
        List<OracleEvidenceEntity> evidences = List.of(
                evidence("YES"), evidence("YES"), evidence("NO"));
        EvidenceAggregationResult result = aggregator.aggregate(evidences);
        assertThat(result.isQuorumReached()).isTrue();
        assertThat(result.getWinningOutcome()).isEqualTo("YES");
        assertThat(result.isRequiresManualReview()).isFalse();
    }

    @Test
    void tieRequiresManualReview() {
        List<OracleEvidenceEntity> evidences = List.of(evidence("YES"), evidence("NO"));
        EvidenceAggregationResult result = aggregator.aggregate(evidences);
        assertThat(result.isQuorumReached()).isFalse();
        assertThat(result.isRequiresManualReview()).isTrue();
    }

    @Test
    void insufficientSources() {
        EvidenceAggregationResult result = aggregator.aggregate(List.of(evidence("YES")));
        assertThat(result.isQuorumReached()).isFalse();
        assertThat(result.isRequiresManualReview()).isTrue();
    }

    private OracleEvidenceEntity evidence(String outcome) {
        return OracleEvidenceEntity.builder()
                .marketId("m1")
                .sourceId(1L)
                .sourceUrl("http://test")
                .fetchedAt(Instant.now())
                .rawPayload(Map.of("outcome", outcome))
                .normalizedOutcomeCode(outcome)
                .hashDigest("abc")
                .build();
    }
}
