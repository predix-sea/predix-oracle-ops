package com.predix.oracle.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "predix.oracle.exchange";
    public static final String QUEUE_JOBS = "predix.oracle.jobs";
    public static final String QUEUE_STATUS_SYNC = "predix.oracle.status-sync";
    public static final String QUEUE_DLQ = "predix.oracle.dlq";

    public static final String ROUTING_JOBS = "oracle.jobs";
    public static final String ROUTING_STATUS_SYNC = "oracle.status-sync";
    public static final String ROUTING_DLQ = "oracle.dlq";

    @Bean
    public TopicExchange oracleExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue jobsQueue() {
        return QueueBuilder.durable(QUEUE_JOBS).build();
    }

    @Bean
    public Queue statusSyncQueue() {
        return QueueBuilder.durable(QUEUE_STATUS_SYNC).build();
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    public Binding jobsBinding(Queue jobsQueue, TopicExchange oracleExchange) {
        return BindingBuilder.bind(jobsQueue).to(oracleExchange).with(ROUTING_JOBS);
    }

    @Bean
    public Binding statusSyncBinding(Queue statusSyncQueue, TopicExchange oracleExchange) {
        return BindingBuilder.bind(statusSyncQueue).to(oracleExchange).with(ROUTING_STATUS_SYNC);
    }

    @Bean
    public Binding dlqBinding(Queue dlqQueue, TopicExchange oracleExchange) {
        return BindingBuilder.bind(dlqQueue).to(oracleExchange).with(ROUTING_DLQ);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
