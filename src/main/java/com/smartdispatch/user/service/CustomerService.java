package com.smartdispatch.user.service;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.user.dto.CustomerResponse;
import com.smartdispatch.user.mapper.CustomerMapper;
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
}
