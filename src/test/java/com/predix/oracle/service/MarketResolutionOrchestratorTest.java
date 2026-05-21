package com.predix.oracle.service;

import com.predix.oracle.client.MarketSchemaClient;
import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.crawler.EvidenceAggregationResult;
import com.predix.oracle.crawler.EvidenceCollectionService;
import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.repository.entity.ResolutionJobEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketResolutionOrchestratorTest {

    @Mock private DistributedLockService lockService;
    @Mock private MarketSchemaClient marketSchemaClient;
    @Mock private EvidenceCollectionService evidenceService;
    @Mock private ResolutionJobService jobService;
    @Mock private ResolutionStateMachine stateMachine;

    @InjectMocks private MarketResolutionOrchestrator orchestrator;

    @Test
    void scanCreatesRequestWhenQuorum() {
        MarketDto closed = MarketDto.builder().marketId("m1").status("CLOSED").build();
        when(marketSchemaClient.listMarketsByStatus("CLOSED")).thenReturn(List.of(closed));
        when(marketSchemaClient.listMarketsByStatus("RESOLVING")).thenReturn(List.of());
        when(lockService.tryWithLock(any(), any(), any(Runnable.class))).thenAnswer(inv -> {
            Runnable r = inv.getArgument(2);
            r.run();
            return true;
        });
        when(evidenceService.collectForMarket(eq("m1"), eq(ActorType.SYSTEM)))
                .thenReturn(EvidenceAggregationResult.builder()
                        .quorumReached(true)
                        .winningOutcome("YES")
                        .agreementRatio(new BigDecimal("1"))
                        .build());
        when(jobService.createJob(any(), any(), any(), any(), any()))
                .thenReturn(ResolutionJobEntity.builder().id(1L).build());

        orchestrator.scanPendingMarkets();

        verify(jobService).createJob(eq("m1"), eq(JobType.REQUEST), any(), any(), eq(ActorType.SYSTEM));
    }
}
