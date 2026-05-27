package com.smartdispatch.user.controller;

import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.user.dto.CustomerResponse;
import com.smartdispatch.user.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getAllCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<CustomerResponse> customers = customerService.getAllCustomers(search, status, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<CustomerResponse>>builder()
                .success(true)
                .message("Customers fetched successfully")
                .data(customers)
                .status(200)
                .build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CustomerResponse>> toggleCustomerStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        CustomerResponse customer = customerService.toggleCustomerStatus(id, active);
        
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true)
                .message("Customer status updated successfully")
                .data(customer)
                .status(200)
                .build());
    }
}
