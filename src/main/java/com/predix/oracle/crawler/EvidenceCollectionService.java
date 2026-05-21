package com.predix.oracle.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.predix.oracle.audit.AuditService;
import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.ResolutionPhase;
import com.predix.oracle.exception.OracleErrorCode;
import com.predix.oracle.exception.OracleException;
import com.predix.oracle.repository.OracleEvidenceRepository;
import com.predix.oracle.repository.OracleSourceRepository;
import com.predix.oracle.repository.entity.OracleEvidenceEntity;
import com.predix.oracle.repository.entity.OracleSourceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EvidenceCollectionService {

    private final OracleSourceRepository sourceRepository;
    private final OracleEvidenceRepository evidenceRepository;
    private final HttpSourceCrawler httpSourceCrawler;
    private final EvidenceAggregator evidenceAggregator;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public EvidenceAggregationResult collectForMarket(String marketId, ActorType actor) {
        List<OracleSourceEntity> sources = sourceRepository.findByEnabledTrueOrderByPriorityAsc();
        if (sources.isEmpty()) {
            throw new OracleException(OracleErrorCode.ORACLE_SOURCE_UNAVAILABLE, "No enabled oracle sources");
        }

        int successCount = 0;
        for (OracleSourceEntity source : sources) {
            try {
                SourceFetchResult fetch = httpSourceCrawler.fetch(source, marketId);
                String digest = digest(marketId, source.getId(), fetch);
                OracleEvidenceEntity evidence = OracleEvidenceEntity.builder()
                        .marketId(marketId)
                        .sourceId(source.getId())
                        .sourceUrl(fetch.getSourceUrl())
                        .fetchedAt(Instant.now())
                        .rawPayload(fetch.getRawPayload())
                        .normalizedOutcomeCode(fetch.getNormalizedOutcome())
                        .confidenceScore(fetch.getConfidenceScore())
                        .hashDigest(digest)
                        .build();
                evidenceRepository.save(evidence);
                successCount++;
            } catch (OracleException ex) {
                // continue other sources; partial evidence is append-only
            }
        }

        List<OracleEvidenceEntity> all = evidenceRepository.findByMarketIdOrderByFetchedAtDesc(marketId);
        EvidenceAggregationResult result = evidenceAggregator.aggregate(all);

        auditService.record(marketId, ResolutionPhase.EVIDENCE_COLLECT.name(), actor,
                "COLLECT_EVIDENCE",
                Map.of("sourcesAttempted", sources.size(), "sourcesSucceeded", successCount),
                Map.of(
                        "quorumReached", result.isQuorumReached(),
                        "winningOutcome", result.getWinningOutcome() != null ? result.getWinningOutcome() : "",
                        "requiresManualReview", result.isRequiresManualReview(),
                        "evidenceCount", all.size()
                ));

        return result;
    }

    public List<OracleEvidenceEntity> listByMarket(String marketId) {
        return evidenceRepository.findByMarketIdOrderByFetchedAtDesc(marketId);
    }

    private String digest(String marketId, Long sourceId, SourceFetchResult fetch) {
        try {
            String payload = marketId + "|" + sourceId + "|" + objectMapper.writeValueAsString(fetch.getRawPayload());
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "digest-error";
        }
    }
}
