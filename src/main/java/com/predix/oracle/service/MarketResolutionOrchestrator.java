package com.predix.oracle.service;

import com.predix.oracle.client.MarketSchemaClient;
import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.crawler.EvidenceAggregationResult;
import com.predix.oracle.crawler.EvidenceCollectionService;
import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.domain.ResolutionPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketResolutionOrchestrator {

    private final DistributedLockService lockService;
    private final MarketSchemaClient marketSchemaClient;
    private final EvidenceCollectionService evidenceService;
    private final ResolutionJobService jobService;
    private final ResolutionStateMachine stateMachine;

    public void scanPendingMarkets() {
        lockService.tryWithLock("scheduler:market-scan", Duration.ofSeconds(55), () -> {
            List<MarketDto> closed = marketSchemaClient.listMarketsByStatus("CLOSED");
            List<MarketDto> resolving = marketSchemaClient.listMarketsByStatus("RESOLVING");
            closed.forEach(this::processClosedMarket);
            resolving.forEach(this::processResolvingMarket);
        });
    }

    private void processClosedMarket(MarketDto market) {
        try {
            stateMachine.validateTransition(market, ResolutionPhase.EVIDENCE_COLLECT);
            EvidenceAggregationResult agg = evidenceService.collectForMarket(market.getMarketId(), ActorType.SYSTEM);
            if (agg.isQuorumReached()) {
                jobService.createJob(market.getMarketId(), JobType.REQUEST,
                        ResolutionJobExecutor.idempotency(market.getMarketId(), JobType.REQUEST),
                        Map.of("auto", true), ActorType.SYSTEM);
            }
        } catch (Exception e) {
            log.debug("Skip market {}: {}", market.getMarketId(), e.getMessage());
        }
    }

    private void processResolvingMarket(MarketDto market) {
        jobService.createJob(market.getMarketId(), JobType.SYNC_STATUS,
                ResolutionJobExecutor.idempotency(market.getMarketId(), JobType.SYNC_STATUS) + "-scan",
                Map.of(), ActorType.SYSTEM);
    }

    public void triggerRequest(String marketId, ActorType actor) {
        MarketDto market = marketSchemaClient.getMarket(marketId);
        stateMachine.validateTransition(market, ResolutionPhase.REQUEST);
        jobService.createJob(marketId, JobType.REQUEST,
                ResolutionJobExecutor.idempotency(marketId, JobType.REQUEST) + "-manual-" + System.currentTimeMillis(),
                Map.of("manual", true), actor);
    }

    public void triggerPropose(String marketId, ActorType actor) {
        jobService.createJob(marketId, JobType.PROPOSE,
                ResolutionJobExecutor.idempotency(marketId, JobType.PROPOSE) + "-manual",
                Map.of("manual", true), actor);
    }

    public void triggerSettle(String marketId, ActorType actor) {
        MarketDto market = marketSchemaClient.getMarket(marketId);
        jobService.createJob(marketId, JobType.SETTLE,
                ResolutionJobExecutor.idempotency(marketId, JobType.SETTLE) + "-manual",
                Map.of("assertionId", market != null ? market.getAssertionId() : ""), actor);
    }
}
