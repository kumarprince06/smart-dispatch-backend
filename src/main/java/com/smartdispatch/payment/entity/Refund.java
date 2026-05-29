package com.smartdispatch.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long paymentId;

    private Long orderId;

    private Double amount;

    private String reason;

    private String status; // INITIATED, PROCESSING, SUCCESS, FAILED

    private String gatewayRefundId;

    private LocalDateTime initiatedAt;

    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        initiatedAt = LocalDateTime.now();
    }
}
