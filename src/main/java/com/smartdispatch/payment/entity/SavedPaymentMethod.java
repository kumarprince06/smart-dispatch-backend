package com.smartdispatch.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_payment_methods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type; // e.g. "card", "upi"

    @Column(nullable = false)
    private String title; // e.g. "HDFC Bank Credit Card"

    private String last4; // e.g. "4321" or UPI ID

    @Column(nullable = false)
    private Boolean isDefault = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
