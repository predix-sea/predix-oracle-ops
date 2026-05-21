package com.predix.oracle.service;

import com.predix.oracle.audit.AuditService;
import com.predix.oracle.client.MatchingEngineClient;
import com.predix.oracle.client.MarketSchemaClient;
import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.crawler.EvidenceAggregationResult;
import com.predix.oracle.crawler.EvidenceCollectionService;
import com.predix.oracle.domain.*;
import com.predix.oracle.exception.OracleErrorCode;
import com.predix.oracle.exception.OracleException;
import com.predix.oracle.mq.OracleJobMessage;
import com.predix.oracle.oracle.UmaAssertionStatus;
import com.predix.oracle.oracle.UmaChainResult;
import com.predix.oracle.oracle.UmaClient;
import com.predix.oracle.repository.ResolutionJobRepository;
import com.predix.oracle.repository.entity.ResolutionJobEntity;
import com.predix.oracle.repository.entity.UmaTransactionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResolutionJobExecutor {

    private final ResolutionJobService jobService;
    private final ResolutionJobRepository jobRepository;
    private final MarketSchemaClient marketSchemaClient;
    private final MatchingEngineClient matchingEngineClient;
    private final EvidenceCollectionService evidenceService;
    private final UmaClient umaClient;
    private final UmaTransactionService umaTransactionService;
    private final ResolutionStateMachine stateMachine;
    private final AuditService auditService;

    @Transactional
    public void execute(OracleJobMessage message) {
        ResolutionJobEntity job = jobService.markRunning(message.getJobId());
        MarketDto market = marketSchemaClient.getMarket(message.getMarketId());
        if (market == null) {
            jobService.markFailed(job.getId(), false);
            throw new OracleException(OracleErrorCode.NOT_FOUND, "Market not found");
        }

        try {
            switch (message.getJobType()) {
                case REQUEST -> handleRequest(job, market, message.getActor());
                case PROPOSE -> handlePropose(job, market, message);
                case DISPUTE -> handleDispute(job, market, message);
                case SETTLE -> handleSettle(job, market, message);
                case SYNC_STATUS -> handleSyncStatus(job, market);
                default -> throw new OracleException(OracleErrorCode.INTERNAL_ERROR, "Unknown job type");
            }
            jobService.markSucceeded(job.getId());
        } catch (OracleException ex) {
            log.warn("Job {} failed: {}", job.getId(), ex.getMessage());
            jobService.markFailed(job.getId(), isRetryable(ex));
            throw ex;
        }
    }

    private void handleRequest(ResolutionJobEntity job, MarketDto market, ActorType actor) {
        stateMachine.validateTransition(market, ResolutionPhase.REQUEST);
        EvidenceAggregationResult agg = evidenceService.collectForMarket(market.getMarketId(), actor);
        if (!agg.isQuorumReached()) {
            throw new OracleException(OracleErrorCode.EVIDENCE_INSUFFICIENT, "Evidence quorum not reached");
        }
        UmaChainResult result = umaClient.requestResolution(market);
        UmaTransactionEntity tx = umaTransactionService.record(market.getMarketId(), UmaActionType.REQUEST, result);
        market.setAssertionId(result.getAssertionId());
        market.setRequestId(result.getRequestId());
        marketSchemaClient.updateMarketStatus(market.getMarketId(), "RESOLVING");
        matchingEngineClient.notifyMarketEvent(market.getMarketId(), "RESOLUTION_REQUESTED");
        auditService.record(market.getMarketId(), ResolutionPhase.REQUEST.name(), actor, "UMA_REQUEST",
                Map.of("evidenceOutcome", agg.getWinningOutcome()),
                Map.of("txHash", tx.getTxHash(), "assertionId", tx.getAssertionId()));

        jobService.createJob(market.getMarketId(), JobType.PROPOSE,
                idempotency(market.getMarketId(), JobType.PROPOSE),
                Map.of("outcome", agg.getWinningOutcome()), ActorType.SYSTEM);
    }

    private void handlePropose(ResolutionJobEntity job, MarketDto market, OracleJobMessage message) {
        stateMachine.validateTransition(market, ResolutionPhase.PROPOSE);
        String outcome = message.getPayload() != null
                ? String.valueOf(message.getPayload().get("outcome"))
                : "YES";
        UmaChainResult result = umaClient.proposeOutcome(market, outcome);
        UmaTransactionEntity tx = umaTransactionService.record(market.getMarketId(), UmaActionType.PROPOSE, result);
        auditService.record(market.getMarketId(), ResolutionPhase.PROPOSE.name(), message.getActor(), "UMA_PROPOSE",
                Map.of("outcome", outcome), Map.of("txHash", tx.getTxHash()));

        jobService.createJob(market.getMarketId(), JobType.SYNC_STATUS,
                idempotency(market.getMarketId(), JobType.SYNC_STATUS),
                Map.of("assertionId", result.getAssertionId()), ActorType.SYSTEM);
    }

    private void handleDispute(ResolutionJobEntity job, MarketDto market, OracleJobMessage message) {
        String assertionId = message.getPayload() != null
                ? String.valueOf(message.getPayload().get("assertionId"))
                : market.getAssertionId();
        UmaChainResult result = umaClient.disputeOutcome(assertionId);
        umaTransactionService.record(market.getMarketId(), UmaActionType.DISPUTE, result);
        auditService.record(market.getMarketId(), ResolutionPhase.DISPUTE.name(), message.getActor(), "UMA_DISPUTE",
                Map.of("assertionId", assertionId), Map.of("txHash", result.getTxHash()));
    }

    private void handleSettle(ResolutionJobEntity job, MarketDto market, OracleJobMessage message) {
        stateMachine.validateTransition(market, ResolutionPhase.SETTLE);
        String assertionId = message.getPayload() != null
                ? String.valueOf(message.getPayload().get("assertionId"))
                : market.getAssertionId();
        UmaChainResult result = umaClient.settleResolution(assertionId);
        UmaTransactionEntity tx = umaTransactionService.record(market.getMarketId(), UmaActionType.SETTLE, result);
        UmaAssertionStatus status = umaClient.getAssertionStatus(assertionId);
        String winning = status.getWinningOutcome() != null ? status.getWinningOutcome() : "YES";
        marketSchemaClient.resolveMarket(market.getMarketId(), winning, assertionId);
        matchingEngineClient.notifyMarketEvent(market.getMarketId(), "MARKET_RESOLVED");
        auditService.record(market.getMarketId(), ResolutionPhase.RESOLVED.name(), message.getActor(), "MARKET_RESOLVED",
                Map.of("assertionId", assertionId), Map.of("winningOutcome", winning, "txHash", tx.getTxHash()));
    }

    private void handleSyncStatus(ResolutionJobEntity job, MarketDto market) {
        String assertionId = market.getAssertionId();
        if (assertionId == null) {
            List<ResolutionJobEntity> proposeJobs = jobRepository.findByMarketIdAndJobTypeAndStatusIn(
                    market.getMarketId(), JobType.PROPOSE, List.of(JobStatus.SUCCEEDED));
            if (!proposeJobs.isEmpty() && proposeJobs.get(0).getPayload() != null) {
                assertionId = String.valueOf(proposeJobs.get(0).getPayload().getOrDefault("assertionId", ""));
            }
        }
        if (assertionId == null) return;

        UmaAssertionStatus status = umaClient.getAssertionStatus(assertionId);
        if (status.isDisputed()) {
            jobService.createJob(market.getMarketId(), JobType.DISPUTE,
                    idempotency(market.getMarketId(), JobType.DISPUTE) + "-auto",
                    Map.of("assertionId", assertionId), ActorType.SYSTEM);
        } else if (status.isSettled() || "PROPOSED".equals(status.getState())) {
            jobService.createJob(market.getMarketId(), JobType.SETTLE,
                    idempotency(market.getMarketId(), JobType.SETTLE),
                    Map.of("assertionId", assertionId), ActorType.SYSTEM);
        }
    }

    private boolean isRetryable(OracleException ex) {
        return ex.getErrorCode() == OracleErrorCode.UMA_REQUEST_FAILED
                || ex.getErrorCode() == OracleErrorCode.UMA_PROPOSE_FAILED
                || ex.getErrorCode() == OracleErrorCode.UMA_SETTLE_FAILED
                || ex.getErrorCode() == OracleErrorCode.ORACLE_SOURCE_UNAVAILABLE;
    }

    public static String idempotency(String marketId, JobType type) {
        return marketId + ":" + type.name();
    }
}
