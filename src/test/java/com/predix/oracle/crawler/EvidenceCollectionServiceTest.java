package com.predix.oracle.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.predix.oracle.audit.AuditService;
import com.predix.oracle.domain.ActorType;
import com.predix.oracle.repository.OracleEvidenceRepository;
import com.predix.oracle.repository.OracleSourceRepository;
import com.predix.oracle.repository.entity.OracleSourceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceCollectionServiceTest {

    @Mock private OracleSourceRepository sourceRepository;
    @Mock private OracleEvidenceRepository evidenceRepository;
    @Mock private HttpSourceCrawler httpSourceCrawler;
    @Mock private EvidenceAggregator evidenceAggregator;
    @Mock private AuditService auditService;

    private EvidenceCollectionService service;

    @BeforeEach
    void setup() {
        service = new EvidenceCollectionService(
                sourceRepository, evidenceRepository, httpSourceCrawler,
                evidenceAggregator, auditService, new ObjectMapper());
    }

    @Test
    void collectsFromAllEnabledSources() {
        OracleSourceEntity s1 = OracleSourceEntity.builder().id(1L).name("a").baseUrl("http://a").build();
        OracleSourceEntity s2 = OracleSourceEntity.builder().id(2L).name("b").baseUrl("http://b").build();
        when(sourceRepository.findByEnabledTrueOrderByPriorityAsc()).thenReturn(List.of(s1, s2));
        when(httpSourceCrawler.fetch(any(), eq("m1"))).thenReturn(SourceFetchResult.builder()
                .sourceUrl("http://a?m1")
                .rawPayload(Map.of("outcome", "YES"))
                .normalizedOutcome("YES")
                .confidenceScore(new BigDecimal("0.9"))
                .build());
        when(evidenceRepository.findByMarketIdOrderByFetchedAtDesc("m1")).thenReturn(List.of());
        when(evidenceAggregator.aggregate(any())).thenReturn(EvidenceAggregationResult.builder()
                .quorumReached(true)
                .winningOutcome("YES")
                .agreementRatio(new BigDecimal("1"))
                .build());

        EvidenceAggregationResult result = service.collectForMarket("m1", ActorType.SYSTEM);

        assertThat(result.isQuorumReached()).isTrue();
        verify(evidenceRepository, times(2)).save(any());
        verify(auditService).record(any(), any(), any(), any(), any(), any());
    }
}
