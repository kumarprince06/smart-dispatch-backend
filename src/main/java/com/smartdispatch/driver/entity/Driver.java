package com.smartdispatch.driver.entity;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.driver.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ═══════════════════════════════════════════
    // Link to Auth User
    // ═══════════════════════════════════════════
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // ═══════════════════════════════════════════
    // Personal Details
    // ═══════════════════════════════════════════
    private String profilePictureUrl;

    private LocalDate dateOfBirth;

    @Column(length = 500)
    private String address;

    private String city;

    private String state;

    private String pincode;

    private String emergencyContactName;

    private String emergencyContactPhone;

    // ═══════════════════════════════════════════
    // Vehicle Information
    // ═══════════════════════════════════════════
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private String vehicleNumber;

    private String vehicleModel;

    private String vehicleColor;

    private Integer vehicleYear;

    private Double vehicleCapacityKg;

    // ═══════════════════════════════════════════
    // Operational State
    // ═══════════════════════════════════════════
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DriverStatus status = DriverStatus.OFFLINE;

    // Real-time location
    private Double currentLatitude;

    private Double currentLongitude;

    private LocalDateTime locationUpdatedAt;

    // ═══════════════════════════════════════════
    // Performance Metrics
    // ═══════════════════════════════════════════
    @Builder.Default
    private Double rating = 0.0;

    @Builder.Default
    private Integer totalRatings = 0;

    @Builder.Default
    private Integer totalTrips = 0;

    @Builder.Default
    private Integer totalDeliveries = 0;

    @Builder.Default
    private Double acceptanceRate = 100.0;

    @Builder.Default
    private Double completionRate = 100.0;

    @Builder.Default
    private Double onTimeRate = 100.0;

    // Composite performance score (calculated)
    @Builder.Default
    private Double performanceScore = 0.0;

    // ═══════════════════════════════════════════
    // Capacity & Dispatch
    // ═══════════════════════════════════════════
    @Builder.Default
    private Integer maxConcurrentOrders = 1;

    @Builder.Default
    private Integer activeOrderCount = 0;

    // ═══════════════════════════════════════════
    // Tier System
    // ═══════════════════════════════════════════
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DriverTier tier = DriverTier.BRONZE;

    // ═══════════════════════════════════════════
    // Financial
    // ═══════════════════════════════════════════
    @Builder.Default
    private Double totalEarnings = 0.0;

    @Builder.Default
    private Double walletBalance = 0.0;

    // ═══════════════════════════════════════════
    // Zone & Service Area
    // ═══════════════════════════════════════════
    private String preferredZone;

    @Builder.Default
    private Double serviceRadiusKm = 10.0;

    // ═══════════════════════════════════════════
    // Verification
    // ═══════════════════════════════════════════
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    private String rejectionReason;

    // ═══════════════════════════════════════════
    // Skill Tags (for smart dispatch matching)
    // ═══════════════════════════════════════════
    @ElementCollection(targetClass = DriverSkillTag.class)
    @CollectionTable(name = "driver_skill_tags", joinColumns = @JoinColumn(name = "driver_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_tag")
    @Builder.Default
    private Set<DriverSkillTag> skillTags = new HashSet<>();

    // ═══════════════════════════════════════════
    // License
    // ═══════════════════════════════════════════
    private String licenseNumber;

    private LocalDate licenseExpiry;

    // ═══════════════════════════════════════════
    // Documents (One-to-Many)
    // ═══════════════════════════════════════════
    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<DriverDocument> documents = new HashSet<>();

    // ═══════════════════════════════════════════
    // Metadata
    // ═══════════════════════════════════════════
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime onboardedAt;

    private LocalDateTime lastActiveAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        onboardedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ═══════════════════════════════════════════
    // Business Methods
    // ═══════════════════════════════════════════

    /**
     * Calculate composite performance score.
     * Weighted: Rating 40% + Acceptance 20% + Completion 25% + OnTime 15%
     */
    public void calculatePerformanceScore() {
        double normalizedRating = (this.rating / 5.0) * 100;
        this.performanceScore =
                (normalizedRating * 0.40) +
                (this.acceptanceRate * 0.20) +
                (this.completionRate * 0.25) +
                (this.onTimeRate * 0.15);
    }

    /**
     * Auto-calculate tier based on performance score.
     */
    public void calculateTier() {
        calculatePerformanceScore();
        if (this.performanceScore >= 90 && this.totalTrips >= 500) {
            this.tier = DriverTier.PLATINUM;
        } else if (this.performanceScore >= 75 && this.totalTrips >= 200) {
            this.tier = DriverTier.GOLD;
        } else if (this.performanceScore >= 60 && this.totalTrips >= 50) {
            this.tier = DriverTier.SILVER;
        } else {
            this.tier = DriverTier.BRONZE;
        }
    }

    /**
     * Check if driver can accept more orders.
     */
    public boolean canAcceptOrder() {
        return this.status == DriverStatus.AVAILABLE
                && this.activeOrderCount < this.maxConcurrentOrders
                && this.verificationStatus == VerificationStatus.VERIFIED
                && this.active;
    }
}
