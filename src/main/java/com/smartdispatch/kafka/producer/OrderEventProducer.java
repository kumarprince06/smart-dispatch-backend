package com.smartdispatch.kafka.producer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdispatch.kafka.event.OrderEvent;
import com.smartdispatch.kafka.outbox.OutboxEvent;
import com.smartdispatch.kafka.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    // Must run within the same transaction as the caller (OrderService)
    @Transactional(propagation = Propagation.REQUIRED)
    public void publishOrderEvent(OrderEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(event.getOrderId().toString())
                    .type("OrderLifecycleEvent")
                    .payload(payload)
                    .createdAt(LocalDateTime.now())
                    .build();
                    
            outboxEventRepository.save(outboxEvent);
            log.debug("Saved OrderEvent to outbox table for orderId=[{}]", event.getOrderId());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderEvent for outbox: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
