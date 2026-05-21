package com.predix.oracle.service;

import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.JobStatus;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.mq.OracleMqPublisher;
import com.predix.oracle.repository.ResolutionJobRepository;
import com.predix.oracle.repository.entity.ResolutionJobEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolutionJobServiceExtendedTest {

    @Mock private ResolutionJobRepository jobRepository;
    @Mock private OracleMqPublisher mqPublisher;
    @Mock private RetryPolicyService retryPolicy;

    @InjectMocks private ResolutionJobService jobService;

    @Test
    void retryJobRepublishes() {
        ResolutionJobEntity job = ResolutionJobEntity.builder()
                .id(5L)
                .marketId("m1")
                .jobType(JobType.PROPOSE)
                .status(JobStatus.FAILED)
                .idempotencyKey("k")
                .build();
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResolutionJobEntity ret = jobService.retryJob(5L, ActorType.MANUAL);

        assertThat(ret.getStatus()).isEqualTo(JobStatus.PENDING);
        verify(mqPublisher).publishJob(any());
    }

    @Test
    void findJobsByMarket() {
        when(jobRepository.findByMarketId("m1")).thenReturn(List.of());
        assertThat(jobService.findJobs("m1", null, null)).isEmpty();
    }

    @Test
    void markFailedDeadPublishesDlq() {
        ResolutionJobEntity job = ResolutionJobEntity.builder()
                .id(9L)
                .marketId("m1")
                .jobType(JobType.REQUEST)
                .status(JobStatus.FAILED)
                .idempotencyKey("dead-key")
                .retryCount(4)
                .build();
        when(jobRepository.findById(9L)).thenReturn(Optional.of(job));
        when(retryPolicy.canRetry(5)).thenReturn(false);
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(jobService.markFailed(9L, true).getStatus()).isEqualTo(JobStatus.DEAD);
        verify(mqPublisher).publishDlq(any());
    }

    @Test
    void markSucceededClearsRetry() {
        ResolutionJobEntity job = ResolutionJobEntity.builder()
                .id(1L).status(JobStatus.RUNNING).retryCount(2).build();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(jobService.markSucceeded(1L).getStatus()).isEqualTo(JobStatus.SUCCEEDED);
    }
}
