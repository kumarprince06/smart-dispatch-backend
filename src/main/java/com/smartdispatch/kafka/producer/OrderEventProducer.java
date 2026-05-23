package com.smartdispatch.kafka.producer;

import com.smartdispatch.config.KafkaConfig;
import com.smartdispatch.kafka.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publishOrderEvent(OrderEvent event) {
        CompletableFuture<SendResult<String, OrderEvent>> future = 
            kafkaTemplate.send(KafkaConfig.ORDER_EVENTS_TOPIC, event.getOrderId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Sent event for order=[{}] with offset=[{}]", event.getOrderId(), result.getRecordMetadata().offset());
            } else {
                log.error("Unable to send event for order=[{}] due to : {}", event.getOrderId(), ex.getMessage());
            }
        });
    }
}
