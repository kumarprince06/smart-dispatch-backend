package com.smartdispatch.driver.mapper;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.driver.dto.CreateDriverRequest;
import com.smartdispatch.driver.dto.DriverResponse;
import com.smartdispatch.driver.entity.Driver;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class DriverMapper {

    // Entity → Response DTO
    public DriverResponse toResponse(Driver driver) {
        User user = driver.getUser();

        return DriverResponse.builder()
                .id(driver.getId())
                // User info
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNo())
                // Personal
                .profilePictureUrl(driver.getProfilePictureUrl())
                .dateOfBirth(driver.getDateOfBirth())
                .address(driver.getAddress())
                .city(driver.getCity())
                .state(driver.getState())
                .pincode(driver.getPincode())
                .emergencyContactName(driver.getEmergencyContactName())
                .emergencyContactPhone(driver.getEmergencyContactPhone())
                // Vehicle
                .vehicleType(driver.getVehicleType())
                .vehicleNumber(driver.getVehicleNumber())
                .vehicleModel(driver.getVehicleModel())
                .vehicleColor(driver.getVehicleColor())
                .vehicleYear(driver.getVehicleYear())
                .vehicleCapacityKg(driver.getVehicleCapacityKg())
                // Operational
                .status(driver.getStatus())
                .currentLatitude(driver.getCurrentLatitude())
                .currentLongitude(driver.getCurrentLongitude())
                .locationUpdatedAt(driver.getLocationUpdatedAt())
                // Performance
                .rating(driver.getRating())
                .totalRatings(driver.getTotalRatings())
                .totalTrips(driver.getTotalTrips())
                .totalDeliveries(driver.getTotalDeliveries())
                .acceptanceRate(driver.getAcceptanceRate())
                .completionRate(driver.getCompletionRate())
                .onTimeRate(driver.getOnTimeRate())
                .performanceScore(driver.getPerformanceScore())
                // Capacity
                .maxConcurrentOrders(driver.getMaxConcurrentOrders())
                .activeOrderCount(driver.getActiveOrderCount())
                // Tier
                .tier(driver.getTier())
                // Financial
                .totalEarnings(driver.getTotalEarnings())
                .walletBalance(driver.getWalletBalance())
                // Zone
                .preferredZone(driver.getPreferredZone())
                .serviceRadiusKm(driver.getServiceRadiusKm())
                // Verification
                .verificationStatus(driver.getVerificationStatus())
                .rejectionReason(driver.getRejectionReason())
                // Skills
                .skillTags(driver.getSkillTags())
                // License
                .licenseNumber(driver.getLicenseNumber())
                .licenseExpiry(driver.getLicenseExpiry())
                // Metadata
                .active(driver.getActive())
                .onboardedAt(driver.getOnboardedAt())
                .lastActiveAt(driver.getLastActiveAt())
                .createdAt(driver.getCreatedAt())
                .build();
    }

    // CreateRequest DTO → Entity
    public Driver toEntity(CreateDriverRequest request, User user) {
        return Driver.builder()
                .user(user)
                // Personal
                .profilePictureUrl(request.getProfilePictureUrl())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                // Vehicle
                .vehicleType(request.getVehicleType())
                .vehicleNumber(request.getVehicleNumber())
                .vehicleModel(request.getVehicleModel())
                .vehicleColor(request.getVehicleColor())
                .vehicleYear(request.getVehicleYear())
                .vehicleCapacityKg(request.getVehicleCapacityKg())
                // License
                .licenseNumber(request.getLicenseNumber())
                .licenseExpiry(request.getLicenseExpiry())
                // Zone
                .preferredZone(request.getPreferredZone())
                .serviceRadiusKm(request.getServiceRadiusKm() != null ? request.getServiceRadiusKm() : 10.0)
                // Capacity
                .maxConcurrentOrders(request.getMaxConcurrentOrders() != null ? request.getMaxConcurrentOrders() : 1)
                // Skill Tags
                .skillTags(request.getSkillTags() != null ? request.getSkillTags() : new HashSet<>())
                .build();
    }
}
