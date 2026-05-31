package com.smartdispatch.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phoneNo;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    // ═══════════════════════════════════════════
    // Profile
    // ═══════════════════════════════════════════
    private String profilePictureUrl;

    private String gender; // MALE, FEMALE, OTHER

    private String dateOfBirth;

    // ═══════════════════════════════════════════
    // Default Address (for quick order)
    // ═══════════════════════════════════════════
    private String defaultAddress;

    private String city;

    private String state;

    private String pincode;

    private Double defaultLatitude;

    private Double defaultLongitude;

    // ═══════════════════════════════════════════
    // Wallet & Payment
    // ═══════════════════════════════════════════
    @Builder.Default
    private Double walletBalance = 0.0;

    // ═══════════════════════════════════════════
    // Loyalty & Engagement
    // ═══════════════════════════════════════════
    @Builder.Default
    private Integer loyaltyPoints = 0;

    @Builder.Default
    private Integer totalOrders = 0;

    @Builder.Default
    private Integer totalSpent = 0; // in rupees

    // ═══════════════════════════════════════════
    // Preferences
    // ═══════════════════════════════════════════
    private String preferredLanguage;

    @Builder.Default
    private Boolean notificationsEnabled = true;

    @Builder.Default
    private Boolean smsEnabled = true;

    @Builder.Default
    private Boolean darkMode = false;

    // Firebase Cloud Messaging Token
    private String fcmToken;

    // ═══════════════════════════════════════════
    // Referral System
    // ═══════════════════════════════════════════
    @Column(unique = true)
    private String referralCode;

    private String referredBy; // referral code of the person who referred

    @Builder.Default
    private Integer referralCount = 0;

    // ═══════════════════════════════════════════
    // Account Status
    // ═══════════════════════════════════════════
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean phoneVerified = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean active = true;

    private String deactivationReason;

    // ═══════════════════════════════════════════
    // Login & Security Tracking
    // ═══════════════════════════════════════════
    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    private String lastLoginDevice;

    // ═══════════════════════════════════════════
    // Audit
    // ═══════════════════════════════════════════
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate(){
        updatedAt = LocalDateTime.now();
    }

}
