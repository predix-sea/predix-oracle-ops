package com.predix.oracle.mq;

import com.predix.oracle.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OracleMqPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishJob(OracleJobMessage message) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_JOBS, message);
    }

    public void publishStatusSync(OracleJobMessage message) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_STATUS_SYNC, message);
    }

    public void publishDlq(OracleJobMessage message) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_DLQ, message);
    }
}
