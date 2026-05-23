package com.smartdispatch.address.controller;

import com.smartdispatch.address.entity.SavedAddress;
import com.smartdispatch.address.repository.SavedAddressRepository;
import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class SavedAddressController {

    private final SavedAddressRepository addressRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<SavedAddress>> addAddress(@RequestBody SavedAddress address) {
        User user = getCurrentUser();

        if (addressRepository.countByUserId(user.getId()) >= 10) {
            throw new BadRequestException("Maximum 10 saved addresses allowed");
        }

        address.setUserId(user.getId());

        // If this is default, unset previous default
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                    .ifPresent(a -> { a.setIsDefault(false); addressRepository.save(a); });
        }

        SavedAddress saved = addressRepository.save(address);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<SavedAddress>builder()
                        .success(true).message("Address saved").status(201).data(saved).build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedAddress>>> getMyAddresses() {
        User user = getCurrentUser();
        List<SavedAddress> addresses = addressRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(ApiResponse.<List<SavedAddress>>builder()
                .success(true).message("Addresses fetched").status(200).data(addresses).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long id) {
        addressRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Address deleted").status(200).build());
    }

    private User getCurrentUser() {
        return userRepository.findByEmail(SecurityUtil.getCurrentUserEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
}
