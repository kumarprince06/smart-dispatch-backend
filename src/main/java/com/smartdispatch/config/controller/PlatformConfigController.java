package com.smartdispatch.config.controller;

import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.config.entity.PlatformConfig;
import com.smartdispatch.config.service.PlatformConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class PlatformConfigController {

    private final PlatformConfigService configService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PlatformConfig>> getConfig() {
        return ResponseEntity.ok(ApiResponse.<PlatformConfig>builder()
                .success(true)
                .message("Settings retrieved")
                .status(200)
                .data(configService.getConfig())
                .build());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PlatformConfig>> updateConfig(@RequestBody PlatformConfig newConfig) {
        return ResponseEntity.ok(ApiResponse.<PlatformConfig>builder()
                .success(true)
                .message("Settings updated successfully")
                .status(200)
                .data(configService.updateConfig(newConfig))
                .build());
    }
}
