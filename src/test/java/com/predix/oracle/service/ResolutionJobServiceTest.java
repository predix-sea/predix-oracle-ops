package com.predix.oracle.service;

import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.JobStatus;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.mq.OracleMqPublisher;
import com.predix.oracle.repository.ResolutionJobRepository;
import com.predix.oracle.repository.entity.ResolutionJobEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolutionJobServiceTest {

    @Mock private ResolutionJobRepository jobRepository;
    @Mock private OracleMqPublisher mqPublisher;
    @Mock private RetryPolicyService retryPolicy;

    @InjectMocks private ResolutionJobService jobService;

    @Test
    void createsJobAndPublishesMessage() {
        when(jobRepository.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
        when(jobRepository.save(any())).thenAnswer(inv -> {
            ResolutionJobEntity e = inv.getArgument(0);
            e.setId(99L);
            return e;
        });

        ResolutionJobEntity job = jobService.createJob("m1", JobType.REQUEST, "k1", Map.of(), ActorType.SYSTEM);

        assertThat(job.getId()).isEqualTo(99L);
        verify(mqPublisher).publishJob(any());
    }

    @Test
    void markFailedSetsRetrying() {
        when(retryPolicy.canRetry(1)).thenReturn(true);
        when(retryPolicy.nextRetryAt(1)).thenReturn(java.time.Instant.now());
        ResolutionJobEntity job = ResolutionJobEntity.builder()
                .id(1L)
                .retryCount(0)
                .marketId("m1")
                .jobType(JobType.REQUEST)
                .idempotencyKey("k")
                .build();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        jobService.markFailed(1L, true);

        ArgumentCaptor<ResolutionJobEntity> captor = ArgumentCaptor.forClass(ResolutionJobEntity.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(JobStatus.RETRYING);
    }
}
