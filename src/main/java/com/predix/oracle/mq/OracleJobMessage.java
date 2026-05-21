package com.predix.oracle.mq;

import com.predix.oracle.domain.ActorType;
import com.predix.oracle.domain.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OracleJobMessage {
    private Long jobId;
    private String marketId;
    private JobType jobType;
    private String idempotencyKey;
    private ActorType actor;
    private Map<String, Object> payload;
}
