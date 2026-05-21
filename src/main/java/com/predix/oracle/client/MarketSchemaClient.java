package com.predix.oracle.client;

import com.predix.oracle.client.dto.MarketDto;
import com.predix.oracle.config.PredixOracleProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketSchemaClient {

    private final RestTemplate restTemplate;
    private final PredixOracleProperties properties;

    private final Map<String, MarketDto> localCache = new ConcurrentHashMap<>();

    public MarketDto getMarket(String marketId) {
        if (!properties.getClients().getMarketSchema().isEnabled()) {
            return localCache.computeIfAbsent(marketId, id -> MarketDto.builder()
                    .marketId(id)
                    .status("CLOSED")
                    .outcomeCodes(List.of("YES", "NO"))
                    .build());
        }
        try {
            String url = properties.getClients().getMarketSchema().getBaseUrl()
                    + "/api/v1/markets/" + marketId;
            return restTemplate.getForObject(url, MarketDto.class);
        } catch (Exception e) {
            log.warn("market-schema unavailable, using cache for {}", marketId);
            return localCache.get(marketId);
        }
    }

    public List<MarketDto> listMarketsByStatus(String status) {
        if (!properties.getClients().getMarketSchema().isEnabled()) {
            return localCache.values().stream()
                    .filter(m -> status.equals(m.getStatus()))
                    .toList();
        }
        try {
            String url = properties.getClients().getMarketSchema().getBaseUrl()
                    + "/api/v1/markets?status=" + status;
            return restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<MarketDto>>() {}).getBody();
        } catch (Exception e) {
            log.warn("Failed to list markets by status {}", status);
            return List.of();
        }
    }

    public void updateMarketStatus(String marketId, String status) {
        MarketDto m = getMarket(marketId);
        if (m != null) {
            m.setStatus(status);
            localCache.put(marketId, m);
        }
        if (!properties.getClients().getMarketSchema().isEnabled()) {
            return;
        }
        try {
            String url = properties.getClients().getMarketSchema().getBaseUrl()
                    + "/api/v1/markets/" + marketId + "/status";
            restTemplate.patchForObject(url, Map.of("status", status), Void.class);
        } catch (Exception e) {
            log.error("Failed to update market status {}", marketId, e);
        }
    }

    public void resolveMarket(String marketId, String winningOutcome, String assertionId) {
        MarketDto m = getMarket(marketId);
        if (m == null) {
            m = MarketDto.builder().marketId(marketId).build();
        }
        m.setStatus("RESOLVED");
        m.setWinningOutcome(winningOutcome);
        m.setAssertionId(assertionId);
        localCache.put(marketId, m);

        if (!properties.getClients().getMarketSchema().isEnabled()) {
            return;
        }
        try {
            String url = properties.getClients().getMarketSchema().getBaseUrl()
                    + "/api/v1/markets/" + marketId + "/resolve";
            restTemplate.postForObject(url, Map.of(
                    "winningOutcome", winningOutcome,
                    "assertionId", assertionId
            ), Void.class);
        } catch (Exception e) {
            log.error("Failed to resolve market {}", marketId, e);
        }
    }

    public void seedMarket(MarketDto market) {
        localCache.put(market.getMarketId(), market);
    }
}
