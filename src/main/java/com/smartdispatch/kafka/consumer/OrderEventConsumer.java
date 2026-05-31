package com.smartdispatch.kafka.consumer;

import com.smartdispatch.config.KafkaConfig;
import com.smartdispatch.kafka.event.OrderEvent;
import com.smartdispatch.notification.service.NotificationService;
import com.smartdispatch.tracking.dto.OrderStatusMessage;
import com.smartdispatch.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final TrackingService trackingService;
    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_TOPIC, groupId = "tracking-group")
    public void consumeForTracking(OrderEvent event) {
        log.info("Received tracking event for order: {}", event.getOrderId());
        
        OrderStatusMessage.OrderStatusMessageBuilder builder = OrderStatusMessage.builder()
                .orderId(event.getOrderId())
                .trackingNumber(event.getTrackingNumber())
                .status(event.getStatus().name())
                .message(event.getMessage());

        if (event.getDriverName() != null) {
            builder.driverName(event.getDriverName())
                    .vehicleNumber(event.getVehicleNumber())
                    .driverLatitude(event.getDriverLatitude())
                    .driverLongitude(event.getDriverLongitude());
        }

        trackingService.broadcastOrderStatus(builder.build());
    }

    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_TOPIC, groupId = "notification-group")
    public void consumeForNotification(OrderEvent event) {
        log.info("Received notification event for order: {}", event.getOrderId());
        
        // Example: Send push notification or email based on the event status
        // This is decoupled from the main thread
        switch(event.getStatus()) {
            case REQUESTED:
                // notificationService.sendEmail(...)
                break;
            case DELIVERED:
                // notificationService.sendEmail(...)
                break;
            default:
                break;
        }
    }
}
