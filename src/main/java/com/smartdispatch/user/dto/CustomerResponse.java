package com.smartdispatch.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNo;
    private String profilePictureUrl;
    private String gender;
    private String dateOfBirth;

    private String city;
    private String state;
    private String pincode;

    private Double walletBalance;
    private Integer loyaltyPoints;
    private Integer totalOrders;
    private Integer totalSpent;

    private Boolean active;
    private Boolean emailVerified;
    private Boolean phoneVerified;

    private Boolean notificationsEnabled;
    private Boolean smsEnabled;
    private Boolean darkMode;
    private String preferredLanguage;

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
