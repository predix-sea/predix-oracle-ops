package com.predix.oracle.oracle;

import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.config.PredixOracleProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "predix.oracle.uma.enabled", havingValue = "false", matchIfMissing = true)
public class StubUmaClient implements UmaClient {

    private final PredixOracleProperties properties;
    private final Map<String, UmaAssertionStatus> assertions = new ConcurrentHashMap<>();

    @Override
    public UmaChainResult requestResolution(MarketDto market) {
        String requestId = "req-" + market.getMarketId();
        String assertionId = "ast-" + market.getMarketId();
        assertions.put(assertionId, UmaAssertionStatus.builder()
                .assertionId(assertionId)
                .requestId(requestId)
                .state("REQUESTED")
                .disputed(false)
                .settled(false)
                .build());
        return tx("REQUEST", assertionId, requestId);
    }

    @Override
    public UmaChainResult proposeOutcome(MarketDto market, String outcome) {
        String assertionId = "ast-" + market.getMarketId();
        UmaAssertionStatus current = assertions.getOrDefault(assertionId,
                UmaAssertionStatus.builder().assertionId(assertionId).requestId("req-" + market.getMarketId()).build());
        assertions.put(assertionId, UmaAssertionStatus.builder()
                .assertionId(assertionId)
                .requestId(current.getRequestId())
                .state("PROPOSED")
                .disputed(false)
                .settled(false)
                .winningOutcome(outcome)
                .build());
        return tx("PROPOSE", assertionId, current.getRequestId());
    }

    @Override
    public UmaChainResult disputeOutcome(String assertionId) {
        UmaAssertionStatus current = assertions.get(assertionId);
        if (current != null) {
            assertions.put(assertionId, UmaAssertionStatus.builder()
                    .assertionId(assertionId)
                    .requestId(current.getRequestId())
                    .state("DISPUTED")
                    .disputed(true)
                    .settled(false)
                    .winningOutcome(current.getWinningOutcome())
                    .build());
        }
        return tx("DISPUTE", assertionId, current != null ? current.getRequestId() : null);
    }

    @Override
    public UmaChainResult settleResolution(String assertionId) {
        UmaAssertionStatus current = assertions.get(assertionId);
        if (current != null) {
            assertions.put(assertionId, UmaAssertionStatus.builder()
                    .assertionId(assertionId)
                    .requestId(current.getRequestId())
                    .state("SETTLED")
                    .disputed(current.isDisputed())
                    .settled(true)
                    .winningOutcome(current.getWinningOutcome())
                    .build());
        }
        return tx("SETTLE", assertionId, current != null ? current.getRequestId() : null);
    }

    @Override
    public UmaAssertionStatus getAssertionStatus(String assertionId) {
        return assertions.getOrDefault(assertionId, UmaAssertionStatus.builder()
                .assertionId(assertionId)
                .state("UNKNOWN")
                .build());
    }

    private UmaChainResult tx(String action, String assertionId, String requestId) {
        String txHash = "0x" + UUID.randomUUID().toString().replace("-", "");
        return UmaChainResult.builder()
                .txHash(txHash)
                .assertionId(assertionId)
                .requestId(requestId)
                .receipt(Map.of(
                        "chainId", properties.getUma().getChainId(),
                        "action", action,
                        "stub", true
                ))
                .build();
    }
}
