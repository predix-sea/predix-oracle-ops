package com.predix.oracle.repository;

import com.predix.oracle.domain.JobStatus;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.repository.entity.ResolutionJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ResolutionJobRepository extends JpaRepository<ResolutionJobEntity, Long> {

    Optional<ResolutionJobEntity> findByIdempotencyKey(String idempotencyKey);

    List<ResolutionJobEntity> findByMarketIdAndStatus(String marketId, JobStatus status);

    List<ResolutionJobEntity> findByMarketId(String marketId);

    @Query("""
            SELECT j FROM ResolutionJobEntity j
            WHERE j.status IN :statuses
            AND j.retryCount < :maxRetries
            AND (j.nextRetryAt IS NULL OR j.nextRetryAt <= :now)
            """)
    List<ResolutionJobEntity> findRetryable(List<JobStatus> statuses, int maxRetries, Instant now);

    List<ResolutionJobEntity> findByMarketIdAndJobTypeAndStatusIn(
            String marketId, JobType jobType, List<JobStatus> statuses);
}
