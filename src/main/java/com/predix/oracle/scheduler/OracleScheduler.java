package com.predix.oracle.scheduler;

import com.predix.oracle.domain.ActorType;
import com.predix.oracle.service.MarketResolutionOrchestrator;
import com.predix.oracle.service.ResolutionJobService;
import com.predix.oracle.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OracleScheduler {

    private final MarketResolutionOrchestrator orchestrator;
    private final ResolutionJobService jobService;
    private final DistributedLockService lockService;

    @Scheduled(cron = "${predix.oracle.scheduler.market-scan-cron}")
    public void scanMarkets() {
        log.debug("Running market scan scheduler");
        orchestrator.scanPendingMarkets();
    }

    @Scheduled(cron = "${predix.oracle.scheduler.retry-cron}")
    public void retryFailedJobs() {
        lockService.tryWithLock("scheduler:retry", Duration.ofSeconds(25), () ->
                jobService.findRetryable().forEach(job ->
                        jobService.retryJob(job.getId(), ActorType.SYSTEM)));
    }
}
