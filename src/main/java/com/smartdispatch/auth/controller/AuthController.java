package com.smartdispatch.auth.controller;

import com.smartdispatch.auth.dto.*;
import com.smartdispatch.auth.service.AuthService;
import com.smartdispatch.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Register User Controller
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(true)
                .message("Your account has been created successfully. Please Login to continue.")
                .status(HttpStatus.CREATED.value())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    // Login User Controller
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {

        LoginResponse loginResponse = authService.login(request);
        ApiResponse<LoginResponse> apiResponse = ApiResponse.<LoginResponse>builder()
                .success(true)
                .message("Login successful")
                .status(HttpStatus.OK.value())
                .data(loginResponse)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    // Refresh Token Controller
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(@RequestBody RefreshTokenRequest request){
        RefreshTokenResponse refreshTokenResponse = authService.refreshToken(request);
        ApiResponse<RefreshTokenResponse> apiResponse = ApiResponse.<RefreshTokenResponse>builder()
                .success(true)
                .message("Token refreshed successfully")
                .status(HttpStatus.OK.value())
                .data(refreshTokenResponse)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    // Logout Controller
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody RefreshTokenRequest request){
        authService.logout(request.getRefreshToken());
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(true)
                .message("Logged out successfully")
                .status(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}
