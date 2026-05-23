package com.smartdispatch.driver.dto;

import com.smartdispatch.driver.enums.DriverSkillTag;
import com.smartdispatch.driver.enums.VehicleType;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDriverRequest {

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

    // License
    private String licenseNumber;
    private LocalDate licenseExpiry;

    // Zone
    private String preferredZone;
    private Double serviceRadiusKm;

    // Capacity
    private Integer maxConcurrentOrders;

    // Skill Tags
    private Set<DriverSkillTag> skillTags;
}
