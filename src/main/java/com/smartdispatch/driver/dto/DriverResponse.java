package com.smartdispatch.driver.dto;

import com.smartdispatch.driver.enums.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponse {

    private Long id;

    // User info
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    // Personal
    private String profilePictureUrl;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String emergencyContactName;
    private String emergencyContactPhone;

    // Vehicle
    private VehicleType vehicleType;
    private String vehicleNumber;
    private String vehicleModel;
    private String vehicleColor;
    private Integer vehicleYear;
    private Double vehicleCapacityKg;

    // Operational
    private DriverStatus status;
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime locationUpdatedAt;

    // Performance
    private Double rating;
    private Integer totalRatings;
    private Integer totalTrips;
    private Integer totalDeliveries;
    private Double acceptanceRate;
    private Double completionRate;
    private Double onTimeRate;
    private Double performanceScore;

    // Capacity
    private Integer maxConcurrentOrders;
    private Integer activeOrderCount;

    // Tier
    private DriverTier tier;

    // Financial
    private Double totalEarnings;
    private Double walletBalance;

    // Zone
    private String preferredZone;
    private Double serviceRadiusKm;

    // Verification
    private VerificationStatus verificationStatus;
    private String rejectionReason;

    // Skills
    private Set<DriverSkillTag> skillTags;

    // License
    private String licenseNumber;
    private LocalDate licenseExpiry;

    // Metadata
    private Boolean active;
    private LocalDateTime onboardedAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
}
