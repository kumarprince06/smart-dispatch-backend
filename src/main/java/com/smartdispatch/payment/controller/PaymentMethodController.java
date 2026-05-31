package com.smartdispatch.payment.controller;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.payment.entity.SavedPaymentMethod;
import com.smartdispatch.payment.repository.SavedPaymentMethodRepository;
import com.smartdispatch.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final SavedPaymentMethodRepository repository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedPaymentMethod>>> getMyPaymentMethods() {
        User user = getCurrentUser();
        List<SavedPaymentMethod> methods = repository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(ApiResponse.<List<SavedPaymentMethod>>builder()
                .success(true).message("Payment methods fetched").status(200).data(methods).build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavedPaymentMethod>> addPaymentMethod(@RequestBody SavedPaymentMethod method) {
        User user = getCurrentUser();
        
        if (repository.countByUserId(user.getId()) >= 5) {
            throw new BadRequestException("Maximum 5 payment methods allowed");
        }
        
        method.setUserId(user.getId());
        
        if (Boolean.TRUE.equals(method.getIsDefault())) {
            repository.findByUserIdAndIsDefaultTrue(user.getId())
                    .ifPresent(m -> { m.setIsDefault(false); repository.save(m); });
        } else if (repository.countByUserId(user.getId()) == 0) {
            method.setIsDefault(true);
        }
        
        SavedPaymentMethod saved = repository.save(method);
        return ResponseEntity.ok(ApiResponse.<SavedPaymentMethod>builder()
                .success(true).message("Payment method saved").status(200).data(saved).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePaymentMethod(@PathVariable Long id) {
        User user = getCurrentUser();
        SavedPaymentMethod method = repository.findById(id)
                .orElseThrow(() -> new BadRequestException("Payment method not found"));
                
        if (!method.getUserId().equals(user.getId())) {
            throw new BadRequestException("Unauthorized");
        }
        
        repository.delete(method);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Payment method deleted").status(200).build());
    }

    private User getCurrentUser() {
        return userRepository.findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
}
