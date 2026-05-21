package com.predix.oracle.service;

import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.JobStatus;
import com.predix.oracle.domain.JobType;
import com.predix.oracle.exception.OracleException;
import com.predix.oracle.mq.OracleMqPublisher;
import com.predix.oracle.repository.ResolutionJobRepository;
import com.predix.oracle.repository.entity.ResolutionJobEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolutionJobIdempotencyTest {

    @Mock
    private ResolutionJobRepository jobRepository;
    @Mock
    private OracleMqPublisher mqPublisher;
    @Mock
    private RetryPolicyService retryPolicy;

    @InjectMocks
    private ResolutionJobService jobService;

    @Test
    void rejectsDuplicateRunningJob() {
        when(jobRepository.findByIdempotencyKey("m1:REQUEST")).thenReturn(Optional.of(
                ResolutionJobEntity.builder()
                        .id(1L)
                        .status(JobStatus.RUNNING)
                        .idempotencyKey("m1:REQUEST")
                        .build()));

        assertThatThrownBy(() -> jobService.createJob("m1", JobType.REQUEST, "m1:REQUEST",
                Map.of(), ActorType.SYSTEM))
                .isInstanceOf(OracleException.class);

        verify(mqPublisher, never()).publishJob(any());
    }
}
