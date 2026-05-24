package com.smartdispatch.kafka.outbox;

import com.smartdispatch.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdispatch.kafka.event.OrderEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedAtIsNullOrderByCreatedAtAsc();

        if (!pendingEvents.isEmpty()) {
            log.info("Found {} pending outbox events. Publishing to Kafka...", pendingEvents.size());
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                // Parse back to OrderEvent so JsonSerializer handles it correctly
                OrderEvent orderEvent = objectMapper.readValue(event.getPayload(), OrderEvent.class);

                kafkaTemplate.send(KafkaConfig.ORDER_EVENTS_TOPIC, event.getAggregateId(), orderEvent)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                // Mark as processed
                                event.setProcessedAt(LocalDateTime.now());
                                outboxEventRepository.save(event);
                                log.debug("Successfully published outbox event {} to Kafka", event.getId());
                            } else {
                                log.error("Failed to publish outbox event {} to Kafka: {}", event.getId(), ex.getMessage());
                                // We don't mark as processed, it will be retried on next poll
                            }
                        });
            } catch (Exception e) {
                log.error("Error processing outbox event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
