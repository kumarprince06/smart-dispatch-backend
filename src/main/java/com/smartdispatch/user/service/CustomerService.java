package com.smartdispatch.user.service;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.user.dto.CustomerResponse;
import com.smartdispatch.user.mapper.CustomerMapper;
import com.smartdispatch.user.dto.UpdateProfileRequest;
import com.smartdispatch.user.dto.UpdatePreferencesRequest;
import com.smartdispatch.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final UserRepository userRepository;
    private final CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(String search, String status, Pageable pageable) {
        log.info("Fetching customers with search: {}, status: {}", search, status);
        Page<User> users = userRepository.findAllCustomers(status, search, pageable);
        return users.map(customerMapper::toResponse);
    }

    @Transactional
    public CustomerResponse toggleCustomerStatus(Long customerId, boolean active) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new BadRequestException("Customer not found with id: " + customerId));

        if (!user.getRole().getName().equals("ROLE_CUSTOMER")) {
            throw new BadRequestException("User is not a customer");
        }

        user.setActive(active);
        if (!active) {
            user.setDeactivationReason("Suspended by Admin");
        } else {
            user.setDeactivationReason(null);
        }

        User updated = userRepository.save(user);
        log.info("Customer {} status toggled to active: {}", customerId, active);

        return customerMapper.toResponse(updated);
    }

    public java.util.Map<String, Long> getCustomerStats() {
        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);
        return java.util.Map.of(
                "totalCustomers", userRepository.countTotalCustomers(),
                "activeCustomers", userRepository.countActiveCustomers(),
                "suspendedCustomers", userRepository.countSuspendedCustomers(),
                "newThisWeek", userRepository.countNewCustomersSince(sevenDaysAgo));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getMyProfile() {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return customerMapper.toResponse(user);
    }

    @Transactional
    public CustomerResponse updateMyProfile(UpdateProfileRequest request) {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNo() != null && !request.getPhoneNo().trim().isEmpty()) {
            user.setPhoneNo(request.getPhoneNo());
        }
        
        User updated = userRepository.save(user);
        return customerMapper.toResponse(updated);
    }

    @Transactional
    public CustomerResponse updateMyPreferences(UpdatePreferencesRequest request) {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (request.getNotificationsEnabled() != null) {
            user.setNotificationsEnabled(request.getNotificationsEnabled());
        }
        if (request.getSmsEnabled() != null) {
            user.setSmsEnabled(request.getSmsEnabled());
        }
        if (request.getDarkMode() != null) {
            user.setDarkMode(request.getDarkMode());
        }
        if (request.getPreferredLanguage() != null) {
            user.setPreferredLanguage(request.getPreferredLanguage());
        }

        User updated = userRepository.save(user);
        return customerMapper.toResponse(updated);
    }
}
