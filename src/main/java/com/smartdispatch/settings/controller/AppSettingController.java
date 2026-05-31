package com.smartdispatch.settings.controller;

import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.settings.entity.AppSetting;
import com.smartdispatch.settings.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/app-settings")
@RequiredArgsConstructor
public class AppSettingController {

    private final AppSettingRepository repository;

    // Public settings for Customer and Driver App
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<AppSetting>>> getPublicSettings() {
        List<AppSetting> settings = repository.findByIsPublicTrue();
        return ResponseEntity.ok(ApiResponse.<List<AppSetting>>builder()
                .success(true).message("Public settings fetched").status(200).data(settings).build());
    }

    // Admin only - Get all settings
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AppSetting>>> getAllSettings() {
        List<AppSetting> settings = repository.findAll();
        return ResponseEntity.ok(ApiResponse.<List<AppSetting>>builder()
                .success(true).message("All settings fetched").status(200).data(settings).build());
    }

    // Admin only - Create or update setting
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AppSetting>> saveSetting(@RequestBody AppSetting setting) {
        AppSetting saved = repository.save(setting);
        return ResponseEntity.ok(ApiResponse.<AppSetting>builder()
                .success(true).message("Setting saved successfully").status(200).data(saved).build());
    }
}
