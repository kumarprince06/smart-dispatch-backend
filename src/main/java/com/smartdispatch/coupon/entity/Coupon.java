package com.smartdispatch.coupon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // e.g., FLAT50, FIRST20

    private String description;

    // Discount type: FLAT or PERCENTAGE
    private String discountType; // FLAT, PERCENTAGE

    private Double discountValue; // 50 (₹50 off) or 20 (20% off)

    private Double maxDiscount; // Max discount for percentage coupons

    private Double minOrderAmount; // Minimum order value to apply

    @Builder.Default
    private Integer maxUsage = 100; // Total times this coupon can be used

    @Builder.Default
    private Integer usedCount = 0;

    @Builder.Default
    private Boolean active = true;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active
                && usedCount < maxUsage
                && (validFrom == null || now.isAfter(validFrom))
                && (validUntil == null || now.isBefore(validUntil));
    }
}
