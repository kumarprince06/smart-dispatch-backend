package com.smartdispatch.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String ORDER_EVENTS_TOPIC = "order-events-topic";
    public static final String ORDER_EVENTS_DLQ_TOPIC = ORDER_EVENTS_TOPIC + ".DLQ";

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Dead Letter Queue topic for failed order events.
     * Messages that fail processing after all retries land here.
     */
    @Bean
    public NewTopic orderEventsDlqTopic() {
        return TopicBuilder.name(ORDER_EVENTS_DLQ_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
