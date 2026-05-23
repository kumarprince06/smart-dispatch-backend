package com.smartdispatch.auth.controller;

import com.smartdispatch.auth.dto.AuthResponse;
import com.smartdispatch.auth.dto.LoginRequest;
import com.smartdispatch.auth.dto.LoginResponse;
import com.smartdispatch.auth.dto.RegisterRequest;
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
    public String register(@Valid @RequestBody RegisterRequest request){
        return authService.register(request);
    }


    // Login User Controller
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request){

        LoginResponse loginResponse = authService.login(request);
        ApiResponse<LoginResponse> apiResponse = ApiResponse.<LoginResponse>builder()
                .success(true)
                .message("Login successful")
                .status(HttpStatus.OK.value())
                .data(loginResponse)
                .build();
        return  ResponseEntity.ok(apiResponse);
    }
}
