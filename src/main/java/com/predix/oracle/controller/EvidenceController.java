package com.predix.oracle.controller;

import com.predix.oracle.config.security.AdminTokenInterceptor;
import com.predix.oracle.controller.dto.ApiResponse;
import com.predix.oracle.crawler.EvidenceAggregationResult;
import com.predix.oracle.crawler.EvidenceCollectionService;
import com.predix.oracle.domain.ActorType;
import com.predix.oracle.repository.entity.OracleEvidenceEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/oracle/evidences")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceCollectionService evidenceService;

    @GetMapping
    public ApiResponse<List<OracleEvidenceEntity>> list(@RequestParam String marketId) {
        return ApiResponse.ok(evidenceService.listByMarket(marketId));
    }

    @PostMapping("/collect")
    public ApiResponse<EvidenceAggregationResult> collect(@RequestParam String marketId,
                                                        HttpServletRequest request) {
        ActorType actor = request.getAttribute(AdminTokenInterceptor.ATTR_ACTOR) != null
                ? ActorType.MANUAL : ActorType.SYSTEM;
        return ApiResponse.ok(evidenceService.collectForMarket(marketId, actor));
    }
}
