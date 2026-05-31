package com.smartdispatch.payment.entity;

import com.smartdispatch.payment.enums.PaymentMethod;
import com.smartdispatch.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transactionId;

    // Idempotency: prevents duplicate payments
    @Column(unique = true)
    private String idempotencyKey;

    private Long orderId;

    private Long customerId;

    private Double amount;

    @Builder.Default
    private String currency = "INR";

    private String gatewayOrderId;     // Razorpay/Stripe order ID
    private String gatewayPaymentId;   // Razorpay/Stripe payment ID

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private String providerTransactionId;

    @Column(name = "payment_url")
    private String paymentUrl;

    private String failureReason;

    // Retry tracking
    @Builder.Default
    private Integer retryCount = 0;

    @Builder.Default
    private Integer maxRetries = 3;

    private LocalDateTime paidAt;

    private LocalDateTime refundedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
