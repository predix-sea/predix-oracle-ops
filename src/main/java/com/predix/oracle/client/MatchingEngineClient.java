package com.predix.oracle.client;

import com.predix.oracle.config.PredixOracleProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingEngineClient {

    private final RestTemplate restTemplate;
    private final PredixOracleProperties properties;

    public void notifyMarketEvent(String marketId, String eventType) {
        if (!properties.getClients().getMatchingEngine().isEnabled()) {
            log.debug("matching-engine disabled, skip event {} for {}", eventType, marketId);
            return;
        }
        try {
            String url = properties.getClients().getMatchingEngine().getBaseUrl()
                    + "/api/v1/events/market";
            restTemplate.postForObject(url, Map.of("marketId", marketId, "event", eventType), Void.class);
        } catch (Exception e) {
            log.warn("Failed to notify matching-engine: {} {}", marketId, eventType);
        }
    }
}
