package com.predix.oracle.service;

import com.predix.oracle.audit.AuditService;
import com.predix.oracle.client.MatchingEngineClient;
import com.predix.oracle.client.MarketSchemaClient;
import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.crawler.EvidenceAggregationResult;
import com.predix.oracle.crawler.EvidenceCollectionService;
import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.domain.UmaActionType;
import com.predix.oracle.mq.OracleJobMessage;
import com.predix.oracle.oracle.UmaAssertionStatus;
import com.predix.oracle.oracle.UmaChainResult;
import com.predix.oracle.oracle.UmaClient;
import com.predix.oracle.repository.ResolutionJobRepository;
import com.predix.oracle.repository.entity.ResolutionJobEntity;
import com.predix.oracle.repository.entity.UmaTransactionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolutionJobExecutorTest {

    @Mock private ResolutionJobService jobService;
    @Mock private ResolutionJobRepository jobRepository;
    @Mock private MarketSchemaClient marketSchemaClient;
    @Mock private MatchingEngineClient matchingEngineClient;
    @Mock private EvidenceCollectionService evidenceService;
    @Mock private UmaClient umaClient;
    @Mock private UmaTransactionService umaTransactionService;
    @Mock private ResolutionStateMachine stateMachine;
    @Mock private AuditService auditService;

    @InjectMocks private ResolutionJobExecutor executor;

    private MarketDto market;
    private ResolutionJobEntity job;

    @BeforeEach
    void setUp() {
        market = MarketDto.builder().marketId("m1").status("CLOSED").build();
        job = ResolutionJobEntity.builder().id(1L).marketId("m1").jobType(JobType.REQUEST).build();
    }

    @Test
    void executesRequestJob() {
        when(jobService.markRunning(1L)).thenReturn(job);
        when(marketSchemaClient.getMarket("m1")).thenReturn(market);
        when(evidenceService.collectForMarket(eq("m1"), any()))
                .thenReturn(EvidenceAggregationResult.builder()
                        .quorumReached(true)
                        .winningOutcome("YES")
                        .agreementRatio(new BigDecimal("1.0"))
                        .build());
        when(umaClient.requestResolution(market)).thenReturn(UmaChainResult.builder()
                .txHash("0xabc")
                .assertionId("ast-m1")
                .requestId("req-m1")
                .receipt(Map.of())
                .build());
        when(umaTransactionService.record(eq("m1"), eq(UmaActionType.REQUEST), any()))
                .thenReturn(UmaTransactionEntity.builder().txHash("0xabc").assertionId("ast-m1").build());
        when(jobService.createJob(any(), any(), any(), any(), any())).thenReturn(job);

        executor.execute(OracleJobMessage.builder()
                .jobId(1L)
                .marketId("m1")
                .jobType(JobType.REQUEST)
                .idempotencyKey("m1:REQUEST")
                .actor(ActorType.SYSTEM)
                .build());

        verify(marketSchemaClient).updateMarketStatus("m1", "RESOLVING");
        verify(jobService).markSucceeded(1L);
    }

    @Test
    void executesSettleJob() {
        market.setStatus("RESOLVING");
        market.setAssertionId("ast-m1");
        when(jobService.markRunning(1L)).thenReturn(job);
        when(marketSchemaClient.getMarket("m1")).thenReturn(market);
        when(umaClient.settleResolution("ast-m1")).thenReturn(UmaChainResult.builder()
                .txHash("0xsettle")
                .assertionId("ast-m1")
                .receipt(Map.of())
                .build());
        when(umaClient.getAssertionStatus("ast-m1")).thenReturn(UmaAssertionStatus.builder()
                .winningOutcome("YES")
                .settled(true)
                .build());
        when(umaTransactionService.record(any(), any(), any()))
                .thenReturn(UmaTransactionEntity.builder().txHash("0xsettle").build());

        executor.execute(OracleJobMessage.builder()
                .jobId(1L)
                .marketId("m1")
                .jobType(JobType.SETTLE)
                .payload(Map.of("assertionId", "ast-m1"))
                .actor(ActorType.MANUAL)
                .build());

        verify(marketSchemaClient).resolveMarket("m1", "YES", "ast-m1");
    }

    @Test
    void executesProposeJob() {
        market.setStatus("RESOLVING");
        job.setJobType(JobType.PROPOSE);
        when(jobService.markRunning(1L)).thenReturn(job);
        when(marketSchemaClient.getMarket("m1")).thenReturn(market);
        when(umaClient.proposeOutcome(eq(market), eq("YES"))).thenReturn(UmaChainResult.builder()
                .txHash("0xp")
                .assertionId("ast-m1")
                .receipt(Map.of())
                .build());
        when(umaTransactionService.record(any(), any(), any()))
                .thenReturn(UmaTransactionEntity.builder().txHash("0xp").build());
        when(jobService.createJob(any(), any(), any(), any(), any())).thenReturn(job);

        executor.execute(OracleJobMessage.builder()
                .jobId(1L)
                .marketId("m1")
                .jobType(JobType.PROPOSE)
                .payload(Map.of("outcome", "YES"))
                .actor(ActorType.SYSTEM)
                .build());

        verify(jobService).markSucceeded(1L);
    }

    @Test
    void executesDisputeJob() {
        market.setStatus("RESOLVING");
        job.setJobType(JobType.DISPUTE);
        when(jobService.markRunning(1L)).thenReturn(job);
        when(marketSchemaClient.getMarket("m1")).thenReturn(market);
        when(umaClient.disputeOutcome("ast-m1")).thenReturn(UmaChainResult.builder()
                .txHash("0xd")
                .assertionId("ast-m1")
                .receipt(Map.of())
                .build());
        when(umaTransactionService.record(any(), any(), any()))
                .thenReturn(UmaTransactionEntity.builder().txHash("0xd").build());

        executor.execute(OracleJobMessage.builder()
                .jobId(1L)
                .marketId("m1")
                .jobType(JobType.DISPUTE)
                .payload(Map.of("assertionId", "ast-m1"))
                .actor(ActorType.MANUAL)
                .build());

        verify(auditService).record(eq("m1"), any(), eq(ActorType.MANUAL), eq("UMA_DISPUTE"), any(), any());
    }

    @Test
    void syncStatusEnqueuesDisputeWhenDisputed() {
        market.setStatus("RESOLVING");
        market.setAssertionId("ast-m1");
        job.setJobType(JobType.SYNC_STATUS);
        when(jobService.markRunning(1L)).thenReturn(job);
        when(marketSchemaClient.getMarket("m1")).thenReturn(market);
        when(umaClient.getAssertionStatus("ast-m1")).thenReturn(UmaAssertionStatus.builder()
                .disputed(true)
                .assertionId("ast-m1")
                .build());
        when(jobService.createJob(any(), any(), any(), any(), any())).thenReturn(job);

        executor.execute(OracleJobMessage.builder()
                .jobId(1L)
                .marketId("m1")
                .jobType(JobType.SYNC_STATUS)
                .idempotencyKey("sync")
                .actor(ActorType.SYSTEM)
                .build());

        verify(jobService).createJob(eq("m1"), eq(JobType.DISPUTE), any(), any(), eq(ActorType.SYSTEM));
    }
}
