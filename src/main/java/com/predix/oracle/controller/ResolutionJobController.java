package com.predix.oracle.controller;

import com.predix.oracle.config.security.AdminTokenInterceptor;
import com.predix.oracle.controller.dto.ApiResponse;
import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.JobStatus;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.repository.entity.ResolutionJobEntity;
import com.predix.oracle.service.MarketResolutionOrchestrator;
import com.predix.oracle.service.ResolutionJobService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/oracle/jobs")
@RequiredArgsConstructor
public class ResolutionJobController {

    private final ResolutionJobService jobService;
    private final MarketResolutionOrchestrator orchestrator;

    @GetMapping
    public ApiResponse<List<ResolutionJobEntity>> list(
            @RequestParam(required = false) String marketId,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) JobType type) {
        return ApiResponse.ok(jobService.findJobs(marketId, status, type));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<ResolutionJobEntity> retry(@PathVariable Long id, HttpServletRequest request) {
        ActorType actor = resolveActor(request);
        return ApiResponse.ok(jobService.retryJob(id, actor));
    }

    @PostMapping("/trigger/request")
    public ApiResponse<Void> triggerRequest(@RequestParam String marketId, HttpServletRequest request) {
        orchestrator.triggerRequest(marketId, resolveActor(request));
        return ApiResponse.ok(null);
    }

    @PostMapping("/trigger/propose")
    public ApiResponse<Void> triggerPropose(@RequestParam String marketId, HttpServletRequest request) {
        orchestrator.triggerPropose(marketId, resolveActor(request));
        return ApiResponse.ok(null);
    }

    @PostMapping("/trigger/settle")
    public ApiResponse<Void> triggerSettle(@RequestParam String marketId, HttpServletRequest request) {
        orchestrator.triggerSettle(marketId, resolveActor(request));
        return ApiResponse.ok(null);
    }

    private ActorType resolveActor(HttpServletRequest request) {
        return request.getAttribute(AdminTokenInterceptor.ATTR_ACTOR) != null
                ? ActorType.MANUAL : ActorType.SYSTEM;
    }
}
