package com.predix.oracle.service;

import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.domain.ResolutionPhase;
import com.predix.oracle.exception.OracleErrorCode;
import com.predix.oracle.exception.OracleException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ResolutionStateMachine {

    private static final Set<String> EVIDENCE_STATUSES = Set.of("CLOSED", "RESOLVING");
    private static final Set<String> REQUEST_STATUSES = Set.of("CLOSED", "RESOLVING");
    private static final Set<String> PROPOSE_STATUSES = Set.of("RESOLVING");
    private static final Set<String> SETTLE_STATUSES = Set.of("RESOLVING");

    public void validateTransition(MarketDto market, ResolutionPhase targetPhase) {
        String status = market.getStatus();
        boolean allowed = switch (targetPhase) {
            case EVIDENCE_COLLECT -> EVIDENCE_STATUSES.contains(status);
            case REQUEST -> REQUEST_STATUSES.contains(status);
            case PROPOSE -> PROPOSE_STATUSES.contains(status);
            case DISPUTE -> PROPOSE_STATUSES.contains(status);
            case SETTLE -> SETTLE_STATUSES.contains(status);
            case SYNC_MARKET, RESOLVED -> true;
        };
        if (!allowed) {
            throw new OracleException(OracleErrorCode.RESOLUTION_STATE_CONFLICT,
                    "Cannot transition market " + market.getMarketId() + " from " + status + " to " + targetPhase);
        }
    }

    public JobType nextJobType(ResolutionPhase phase) {
        return switch (phase) {
            case REQUEST -> JobType.REQUEST;
            case PROPOSE -> JobType.PROPOSE;
            case DISPUTE -> JobType.DISPUTE;
            case SETTLE -> JobType.SETTLE;
            case SYNC_MARKET -> JobType.SYNC_STATUS;
            default -> throw new OracleException(OracleErrorCode.RESOLUTION_STATE_CONFLICT, "No job for phase " + phase);
        };
    }
}
