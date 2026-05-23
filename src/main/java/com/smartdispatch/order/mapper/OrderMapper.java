package com.smartdispatch.order.mapper;

import com.smartdispatch.order.dto.OrderResponse;
import com.smartdispatch.order.entity.Order;
import com.smartdispatch.order.entity.OrderTimeline;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        OrderResponse.OrderResponseBuilder builder = OrderResponse.builder()
                .orderId(order.getId())
                .trackingNumber(order.getTrackingNumber())
                // Customer
                .customerName(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName())
                .customerEmail(order.getCustomer().getEmail())
                .customerPhone(order.getCustomer().getPhoneNo())
                // Pickup
                .pickupAddress(order.getPickupAddress())
                .pickupLatitude(order.getPickupLatitude())
                .pickupLongitude(order.getPickupLongitude())
                .pickupContactName(order.getPickupContactName())
                .pickupContactPhone(order.getPickupContactPhone())
                // Drop
                .dropAddress(order.getDropAddress())
                .dropLatitude(order.getDropLatitude())
                .dropLongitude(order.getDropLongitude())
                .dropContactName(order.getDropContactName())
                .dropContactPhone(order.getDropContactPhone())
                // Package
                .packageType(order.getPackageType())
                .packageDescription(order.getPackageDescription())
                .packageWeightKg(order.getPackageWeightKg())
                // Status
                .status(order.getStatus())
                .priority(order.getPriority())
                // Pricing
                .deliveryFee(order.getDeliveryFee())
                .distanceKm(order.getDistanceKm())
                // OTP
                .pickupOtp(order.getPickupOtp())
                .deliveryOtp(order.getDeliveryOtp())
                // Cancellation
                .cancellationReason(order.getCancellationReason())
                .cancelledBy(order.getCancelledBy())
                // Notes & Rating
                .customerNotes(order.getCustomerNotes())
                .customerRating(order.getCustomerRating())
                .customerFeedback(order.getCustomerFeedback())
                // Timestamps
                .assignedAt(order.getAssignedAt())
                .pickedUpAt(order.getPickedUpAt())
                .inTransitAt(order.getInTransitAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .estimatedDeliveryAt(order.getEstimatedDeliveryAt())
                .createdAt(order.getCreatedAt());

        // Driver (may be null if not yet assigned)
        if (order.getDriver() != null) {
            builder.driverId(order.getDriver().getId())
                    .driverName(order.getDriver().getUser().getFirstName() + " " + order.getDriver().getUser().getLastName())
                    .driverPhone(order.getDriver().getUser().getPhoneNo())
                    .vehicleNumber(order.getDriver().getVehicleNumber());
        }

        // Timeline
        if (order.getTimeline() != null && !order.getTimeline().isEmpty()) {
            List<OrderResponse.TimelineEntry> timelineEntries = order.getTimeline().stream()
                    .map(this::toTimelineEntry)
                    .toList();
            builder.timeline(timelineEntries);
        }

        return builder.build();
    }

    private OrderResponse.TimelineEntry toTimelineEntry(OrderTimeline timeline) {
        return OrderResponse.TimelineEntry.builder()
                .status(timeline.getStatus())
                .description(timeline.getDescription())
                .updatedBy(timeline.getUpdatedBy())
                .timestamp(timeline.getTimestamp())
                .build();
    }
}
