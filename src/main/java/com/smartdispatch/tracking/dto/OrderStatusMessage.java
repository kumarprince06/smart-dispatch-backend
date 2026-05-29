package com.smartdispatch.tracking.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusMessage {

    private Long orderId;
    private String trackingNumber;
    private String status;
    private String message;       // Human-readable: "Driver is on the way"
    private String driverName;
    private String vehicleNumber;
    private Double driverLatitude;
    private Double driverLongitude;
    private Double estimatedMinutes;
    private LocalDateTime timestamp;
}
