package com.smartdispatch.order.entity;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.driver.entity.Driver;
import com.smartdispatch.order.enums.OrderPriority;
import com.smartdispatch.order.enums.OrderStatus;
import com.smartdispatch.order.enums.PackageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique tracking number (e.g., SD-a1b2c3d4)
    @Column(unique = true, nullable = false, updatable = false)
    private String trackingNumber;

    // ═══════════════════════════════════════════
    // Customer & Driver
    // ═══════════════════════════════════════════
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    // ═══════════════════════════════════════════
    // Pickup Details
    // ═══════════════════════════════════════════
    @Column(nullable = false)
    private String pickupAddress;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private String pickupContactName;

    private String pickupContactPhone;

    // ═══════════════════════════════════════════
    // Drop Details
    // ═══════════════════════════════════════════
    @Column(nullable = false)
    private String dropAddress;

    private Double dropLatitude;

    private Double dropLongitude;

    private String dropContactName;

    private String dropContactPhone;

    // ═══════════════════════════════════════════
    // Package Details
    // ═══════════════════════════════════════════
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PackageType packageType = PackageType.SMALL_PARCEL;

    private String packageDescription;

    private Double packageWeightKg;

    // ═══════════════════════════════════════════
    // Order State
    // ═══════════════════════════════════════════
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.REQUESTED;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderPriority priority = OrderPriority.STANDARD;

    // ═══════════════════════════════════════════
    // Pricing
    // ═══════════════════════════════════════════
    @Builder.Default
    private Double deliveryFee = 0.0;

    private Double distanceKm;

    @Builder.Default
    private Double surgeMultiplier = 1.0;

    private String couponCode;

    private Double discountAmount;

    // ═══════════════════════════════════════════
    // Scheduling
    // ═══════════════════════════════════════════
    @Builder.Default
    private Boolean isScheduled = false;

    private LocalDateTime scheduledAt; // null = immediate, set = future delivery

    // ═══════════════════════════════════════════
    // OTP Verification
    // ═══════════════════════════════════════════
    private String pickupOtp;

    private String deliveryOtp;

    // ═══════════════════════════════════════════
    // Cancellation
    // ═══════════════════════════════════════════
    private String cancellationReason;

    private String cancelledBy; // CUSTOMER or DRIVER or ADMIN

    // ═══════════════════════════════════════════
    // Customer Notes
    // ═══════════════════════════════════════════
    @Column(length = 1000)
    private String customerNotes;

    // ═══════════════════════════════════════════
    // Rating (after delivery)
    // ═══════════════════════════════════════════
    private String proofOfDeliveryUrl;

    private Double customerRating;

    private String customerFeedback;

    // ═══════════════════════════════════════════
    // Timeline (Order History)
    // ═══════════════════════════════════════════
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderTimeline> timeline = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // ═══════════════════════════════════════════
    // Audit Timestamps
    // ═══════════════════════════════════════════
    private LocalDateTime assignedAt;

    private LocalDateTime pickedUpAt;

    private LocalDateTime inTransitAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime estimatedDeliveryAt;

    // ═══════════════════════════════════════════
    // Driver Assignment Retry
    // ═══════════════════════════════════════════
    @Builder.Default
    private Integer assignmentAttempts = 0;

    @Builder.Default
    private Integer maxAssignmentAttempts = 3;

    private LocalDateTime lastAssignmentAttemptAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (trackingNumber == null) {
            trackingNumber = "SD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (status == null) {
            status = OrderStatus.REQUESTED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
