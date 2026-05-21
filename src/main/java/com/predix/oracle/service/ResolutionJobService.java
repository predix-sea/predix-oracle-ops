package com.predix.oracle.service;

import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.JobStatus;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.exception.OracleErrorCode;
import com.predix.oracle.exception.OracleException;
import com.predix.oracle.mq.OracleJobMessage;
import com.predix.oracle.mq.OracleMqPublisher;
import com.predix.oracle.repository.ResolutionJobRepository;
import com.predix.oracle.repository.entity.ResolutionJobEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResolutionJobService {

    private final ResolutionJobRepository jobRepository;
    private final OracleMqPublisher mqPublisher;
    private final RetryPolicyService retryPolicy;

    @Transactional
    public ResolutionJobEntity createJob(String marketId, JobType type, String idempotencyKey,
                                         Map<String, Object> payload, ActorType actor) {
        jobRepository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
            if (existing.getStatus() == JobStatus.RUNNING || existing.getStatus() == JobStatus.SUCCEEDED) {
                throw new OracleException(OracleErrorCode.JOB_IDEMPOTENCY_CONFLICT,
                        "Job already exists: " + idempotencyKey);
            }
        });

        ResolutionJobEntity job = ResolutionJobEntity.builder()
                .jobCode("JOB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .marketId(marketId)
                .jobType(type)
                .status(JobStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .retryCount(0)
                .payload(payload)
                .build();

        try {
            job = jobRepository.save(job);
        } catch (DataIntegrityViolationException e) {
            return jobRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new OracleException(OracleErrorCode.JOB_IDEMPOTENCY_CONFLICT,
                            "Duplicate job"));
        }

        mqPublisher.publishJob(OracleJobMessage.builder()
                .jobId(job.getId())
                .marketId(marketId)
                .jobType(type)
                .idempotencyKey(idempotencyKey)
                .actor(actor)
                .payload(payload)
                .build());
        return job;
    }

    @Transactional
    public ResolutionJobEntity markRunning(Long jobId) {
        ResolutionJobEntity job = getJob(jobId);
        job.setStatus(JobStatus.RUNNING);
        return jobRepository.save(job);
    }

    @Transactional
    public ResolutionJobEntity markSucceeded(Long jobId) {
        ResolutionJobEntity job = getJob(jobId);
        job.setStatus(JobStatus.SUCCEEDED);
        job.setNextRetryAt(null);
        return jobRepository.save(job);
    }

    @Transactional
    public ResolutionJobEntity markFailed(Long jobId, boolean retryable) {
        ResolutionJobEntity job = getJob(jobId);
        job.setRetryCount(job.getRetryCount() + 1);
        if (retryable && retryPolicy.canRetry(job.getRetryCount())) {
            job.setStatus(JobStatus.RETRYING);
            job.setNextRetryAt(retryPolicy.nextRetryAt(job.getRetryCount()));
        } else {
            job.setStatus(JobStatus.DEAD);
            mqPublisher.publishDlq(OracleJobMessage.builder()
                    .jobId(job.getId())
                    .marketId(job.getMarketId())
                    .jobType(job.getJobType())
                    .idempotencyKey(job.getIdempotencyKey())
                    .build());
        }
        return jobRepository.save(job);
    }

    @Transactional
    public ResolutionJobEntity retryJob(Long jobId, ActorType actor) {
        ResolutionJobEntity job = getJob(jobId);
        if (job.getStatus() == JobStatus.SUCCEEDED) {
            throw new OracleException(OracleErrorCode.RESOLUTION_STATE_CONFLICT, "Job already succeeded");
        }
        job.setStatus(JobStatus.PENDING);
        job.setNextRetryAt(null);
        jobRepository.save(job);
        mqPublisher.publishJob(OracleJobMessage.builder()
                .jobId(job.getId())
                .marketId(job.getMarketId())
                .jobType(job.getJobType())
                .idempotencyKey(job.getIdempotencyKey())
                .actor(actor)
                .payload(job.getPayload())
                .build());
        return job;
    }

    public List<ResolutionJobEntity> findJobs(String marketId, JobStatus status, JobType type) {
        if (marketId != null && status != null) {
            return jobRepository.findByMarketIdAndStatus(marketId, status);
        }
        if (marketId != null) {
            return jobRepository.findByMarketId(marketId);
        }
        return jobRepository.findAll();
    }

    public ResolutionJobEntity getJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new OracleException(OracleErrorCode.NOT_FOUND, "Job not found: " + id));
    }

    public List<ResolutionJobEntity> findRetryable() {
        return jobRepository.findRetryable(
                List.of(JobStatus.FAILED, JobStatus.RETRYING),
                retryPolicy.maxAttempts(),
                Instant.now());
    }
}
