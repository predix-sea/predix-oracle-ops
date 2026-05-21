package com.predix.oracle.mq;

import com.predix.oracle.config.RabbitMqConfig;
import com.predix.oracle.service.ResolutionJobExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OracleJobConsumer {

    private static final String CONSUMED_PREFIX = "predix:oracle:consumed:";

    private final ResolutionJobExecutor jobExecutor;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_JOBS)
    public void onJob(OracleJobMessage message) {
        String dedupeKey = CONSUMED_PREFIX + message.getIdempotencyKey();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", Duration.ofHours(24));
        if (!Boolean.TRUE.equals(first)) {
            log.info("Skip duplicate message {}", message.getIdempotencyKey());
            return;
        }
        try {
            jobExecutor.execute(message);
        } catch (Exception e) {
            log.error("Job execution failed jobId={}", message.getJobId(), e);
            redisTemplate.delete(dedupeKey);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_STATUS_SYNC)
    public void onStatusSync(OracleJobMessage message) {
        onJob(message);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_DLQ)
    public void onDlq(OracleJobMessage message) {
        log.error("Job moved to DLQ marketId={} jobType={} jobId={}",
                message.getMarketId(), message.getJobType(), message.getJobId());
    }
}
