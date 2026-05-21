package com.predix.oracle.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.predix.oracle.exception.OracleErrorCode;
import com.predix.oracle.exception.OracleException;
import com.predix.oracle.repository.entity.OracleSourceEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpSourceCrawler {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SourceFetchResult fetch(OracleSourceEntity source, String marketId) {
        String url = UriComponentsBuilder.fromHttpUrl(source.getBaseUrl())
                .queryParam("marketId", marketId)
                .toUriString();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new OracleException(OracleErrorCode.ORACLE_SOURCE_UNAVAILABLE,
                        "Source returned non-2xx: " + source.getName());
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            String outcome = root.path("outcome").asText(null);
            if (outcome == null || outcome.isBlank()) {
                outcome = root.path("result").asText("UNKNOWN");
            }
            BigDecimal confidence = null;
            if (root.has("confidence")) {
                confidence = new BigDecimal(root.get("confidence").asText("1"));
            }
            Map<String, Object> raw = objectMapper.convertValue(root, Map.class);
            return SourceFetchResult.builder()
                    .sourceUrl(url)
                    .rawPayload(raw != null ? raw : new HashMap<>())
                    .normalizedOutcome(outcome)
                    .confidenceScore(confidence)
                    .build();
        } catch (OracleException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to fetch source {} for market {}", source.getName(), marketId, ex);
            throw new OracleException(OracleErrorCode.ORACLE_SOURCE_UNAVAILABLE,
                    "Failed to fetch source: " + source.getName(), ex);
        }
    }
}
