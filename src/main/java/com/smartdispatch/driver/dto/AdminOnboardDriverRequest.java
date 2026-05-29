package com.smartdispatch.driver.dto;

import com.smartdispatch.driver.enums.DriverSkillTag;
import com.smartdispatch.driver.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOnboardDriverRequest {

    // User details (to create the User account)
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNo;

    // Driver Personal details
    private String profilePictureUrl;
    private LocalDate dateOfBirth;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String emergencyContactName;
    private String emergencyContactPhone;

    // Vehicle details
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;

    private String vehicleModel;
    private String vehicleColor;
    private Integer vehicleYear;
    private Double vehicleCapacityKg;

    // License details
    @NotBlank(message = "License number is required")
    private String licenseNumber;

    private LocalDate licenseExpiry;

    // Zone & Capacity
    private String preferredZone;
    private Double serviceRadiusKm;
    private Integer maxConcurrentOrders;
    private Set<DriverSkillTag> skillTags;
}
