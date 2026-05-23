package com.smartdispatch.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks provider health metrics for intelligent routing.
 * If a provider has low success rate, orchestrator routes to alternatives.
 */
@Entity
@Table(name = "payment_provider_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String provider; // RAZORPAY, STRIPE, PAYPAL, CASHFREE

    @Builder.Default
    private Long totalRequests = 0L;

    @Builder.Default
    private Long successCount = 0L;

    @Builder.Default
    private Long failureCount = 0L;

    @Builder.Default
    private Double avgLatencyMs = 0.0;

    @Builder.Default
    private Double successRate = 100.0;

    private LocalDateTime lastFailureAt;

    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (totalRequests > 0) {
            successRate = (successCount * 100.0) / totalRequests;
        }
    }

    public void recordSuccess(long latencyMs) {
        totalRequests++;
        successCount++;
        avgLatencyMs = ((avgLatencyMs * (totalRequests - 1)) + latencyMs) / totalRequests;
    }

    public void recordFailure() {
        totalRequests++;
        failureCount++;
        lastFailureAt = LocalDateTime.now();
    }
}
