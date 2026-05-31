package com.smartdispatch.user.controller;

import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.user.dto.CustomerResponse;
import com.smartdispatch.user.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.smartdispatch.user.dto.UpdateProfileRequest;
import com.smartdispatch.user.dto.UpdatePreferencesRequest;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class UserController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> getMyProfile() {
        CustomerResponse profile = customerService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true).message("Profile fetched").status(200).data(profile).build());
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateMyProfile(
            @RequestBody UpdateProfileRequest request
    ) {
        CustomerResponse profile = customerService.updateMyProfile(request);
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true).message("Profile updated successfully").status(200).data(profile).build());
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateMyPreferences(
            @RequestBody UpdatePreferencesRequest request
    ) {
        CustomerResponse profile = customerService.updateMyPreferences(request);
        return ResponseEntity.ok(ApiResponse.<CustomerResponse>builder()
                .success(true).message("Preferences updated successfully").status(200).data(profile).build());
    }
}
