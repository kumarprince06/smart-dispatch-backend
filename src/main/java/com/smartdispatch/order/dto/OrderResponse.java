package com.smartdispatch.order.dto;

import com.smartdispatch.order.enums.OrderPriority;
import com.smartdispatch.order.enums.OrderStatus;
import com.smartdispatch.order.enums.PackageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private String trackingNumber;

    // Customer
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Driver
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private String vehicleNumber;

    // Pickup
    private String pickupAddress;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupContactName;
    private String pickupContactPhone;

    // Drop
    private String dropAddress;
    private Double dropLatitude;
    private Double dropLongitude;
    private String dropContactName;
    private String dropContactPhone;

    // Package
    private PackageType packageType;
    private String packageDescription;
    private Double packageWeightKg;

    // Status
    private OrderStatus status;
    private OrderPriority priority;

    // Pricing
    private Double deliveryFee;
    private Double distanceKm;

    // OTP
    private String pickupOtp;
    private String deliveryOtp;

    // Cancellation
    private String cancellationReason;
    private String cancelledBy;

    // Notes, Proof & Rating
    private String customerNotes;
    private String proofOfDeliveryUrl;
    private Double customerRating;
    private String customerFeedback;

    // Timeline
    private List<TimelineEntry> timeline;

    // Timestamps
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime inTransitAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime estimatedDeliveryAt;
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEntry {
        private OrderStatus status;
        private String description;
        private String updatedBy;
        private LocalDateTime timestamp;
    }
}
