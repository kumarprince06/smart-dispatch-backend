package com.smartdispatch.user.mapper;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.user.dto.CustomerResponse;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return CustomerResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNo(user.getPhoneNo())
                .profilePictureUrl(user.getProfilePictureUrl())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .city(user.getCity())
                .state(user.getState())
                .pincode(user.getPincode())
                .walletBalance(user.getWalletBalance())
                .loyaltyPoints(user.getLoyaltyPoints())
                .totalOrders(user.getTotalOrders())
                .totalSpent(user.getTotalSpent())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
