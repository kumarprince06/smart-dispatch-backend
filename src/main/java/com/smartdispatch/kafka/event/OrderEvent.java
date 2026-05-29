package com.smartdispatch.kafka.event;

import com.smartdispatch.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String eventId;
    private Long orderId;
    private String trackingNumber;
    private OrderStatus status;
    private String customerEmail;
    private String customerName;
    private String driverName;
    private Double driverLatitude;
    private Double driverLongitude;
    private String vehicleNumber;
    private LocalDateTime timestamp;
    private String message;
}
