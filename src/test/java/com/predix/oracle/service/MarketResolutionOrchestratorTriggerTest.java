package com.predix.oracle.service;

import com.predix.oracle.client.MarketSchemaClient;
import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.crawler.EvidenceCollectionService;
import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.repository.entity.ResolutionJobEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketResolutionOrchestratorTriggerTest {

    @Mock private DistributedLockService lockService;
    @Mock private MarketSchemaClient marketSchemaClient;
    @Mock private EvidenceCollectionService evidenceService;
    @Mock private ResolutionJobService jobService;
    @Mock private ResolutionStateMachine stateMachine;

    @InjectMocks private MarketResolutionOrchestrator orchestrator;

    @Test
    void triggerRequestCreatesJob() {
        MarketDto market = MarketDto.builder().marketId("m2").status("CLOSED").build();
        when(marketSchemaClient.getMarket("m2")).thenReturn(market);
        when(jobService.createJob(any(), any(), any(), any(), any()))
                .thenReturn(ResolutionJobEntity.builder().id(1L).build());

        orchestrator.triggerRequest("m2", ActorType.MANUAL);

        verify(jobService).createJob(eq("m2"), eq(JobType.REQUEST), any(), any(), eq(ActorType.MANUAL));
    }

    @Test
    void triggerSettleCreatesJob() {
        MarketDto market = MarketDto.builder().marketId("m2").status("RESOLVING").assertionId("ast").build();
        when(marketSchemaClient.getMarket("m2")).thenReturn(market);
        when(jobService.createJob(any(), any(), any(), any(), any()))
                .thenReturn(ResolutionJobEntity.builder().id(2L).build());

        orchestrator.triggerSettle("m2", ActorType.MANUAL);

        verify(jobService).createJob(eq("m2"), eq(JobType.SETTLE), any(), any(), eq(ActorType.MANUAL));
    }
}
