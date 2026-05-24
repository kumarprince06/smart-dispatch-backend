package com.smartdispatch.config;

import com.smartdispatch.kafka.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer Configuration with Dead Letter Queue (DLQ) support.
 *
 * HOW IT WORKS:
 * 1. A consumer receives a message from a topic (e.g., "order-events-topic").
 * 2. If the consumer throws an exception while processing, the DefaultErrorHandler
 *    catches it and retries the message up to MAX_RETRY_ATTEMPTS times.
 * 3. Between each retry, it waits RETRY_BACKOFF_MS milliseconds.
 * 4. If ALL retries are exhausted and the message still fails, the
 *    DeadLetterPublishingRecoverer routes the message to a ".DLQ" topic
 *    (e.g., "order-events-topic.DLQ").
 * 5. The original consumer partition is NOT blocked — processing continues
 *    for other messages.
 * 6. An admin can later inspect DLQ messages via the DlqController and
 *    replay them back to the original topic.
 */
@Configuration
@Slf4j
public class KafkaConsumerConfig {

    private static final long MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 2000; // 2 seconds between retries

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * A dedicated KafkaTemplate used ONLY by the DLQ recoverer
     * to publish failed messages to the dead letter topic.
     * Uses raw byte[] serializers so it can forward any payload as-is.
     */
    @Bean
    public KafkaTemplate<String, Object> dlqKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        ProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(factory);
    }

    /**
     * Primary KafkaTemplate for publishing OrderEvents to Kafka.
     * Used by OutboxEventScheduler to send events from the outbox table.
     */
    @Bean
    @Primary
    public KafkaTemplate<String, OrderEvent> kafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        ProducerFactory<String, OrderEvent> factory = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(factory);
    }

    /**
     * The core error handler for all Kafka consumers.
     *
     * DeadLetterPublishingRecoverer: When retries are exhausted, it takes the
     * failed ConsumerRecord and publishes it to "{original-topic}.DLQ".
     *
     * DefaultErrorHandler: Spring Kafka's built-in error handler that supports
     * configurable retry with backoff, and delegates to a recoverer on exhaustion.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> dlqKafkaTemplate) {
        // This recoverer sends failed messages to {topic}.DLQ
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dlqKafkaTemplate,
                (record, exception) -> {
                    // Route to a topic named "{originalTopic}.DLQ"
                    log.error("Message exhausted all retries. Routing to DLQ. Topic: {}, Key: {}, Error: {}",
                            record.topic(), record.key(), exception.getMessage());
                    return new org.apache.kafka.common.TopicPartition(
                            record.topic() + ".DLQ", record.partition());
                });

        // FixedBackOff(intervalMs, maxAttempts)
        // After 3 failed attempts with 2s gap, message goes to DLQ
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(RETRY_BACKOFF_MS, MAX_RETRY_ATTEMPTS));

        // Log each retry attempt
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Retry attempt {} for topic={}, key={}, error={}",
                        deliveryAttempt, record.topic(), record.key(), ex.getMessage()));

        return errorHandler;
    }

    /**
     * Consumer factory with JSON deserialization.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * The listener container factory that ties together:
     * - The consumer factory (how to read messages)
     * - The error handler (what to do when processing fails)
     *
     * All @KafkaListener methods will use this factory automatically.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            CommonErrorHandler kafkaErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);

        return factory;
    }
}
