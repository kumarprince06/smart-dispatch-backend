package com.smartdispatch.driver.controller;

import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.driver.dto.*;
import com.smartdispatch.driver.enums.*;
import com.smartdispatch.driver.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    // ═══════════════════════════════════════════
    // Onboard New Driver
    // ═══════════════════════════════════════════
    @PostMapping
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(
            @Valid @RequestBody CreateDriverRequest request
    ) {
        DriverResponse driver = driverService.createDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<DriverResponse>builder()
                        .success(true)
                        .message("Driver onboarded successfully")
                        .status(HttpStatus.CREATED.value())
                        .data(driver)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Admin Onboard New Driver (Creates User + Driver)
    // ═══════════════════════════════════════════
    @PostMapping("/admin/onboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriverResponse>> adminOnboardDriver(
            @Valid @RequestBody AdminOnboardDriverRequest request
    ) {
        DriverResponse driver = driverService.adminOnboardDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<DriverResponse>builder()
                        .success(true)
                        .message("Driver onboarded successfully via admin panel")
                        .status(HttpStatus.CREATED.value())
                        .data(driver)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Get Driver by ID
    // ═══════════════════════════════════════════
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(@PathVariable Long id) {
        DriverResponse driver = driverService.getDriverById(id);
        return ResponseEntity.ok(
                ApiResponse.<DriverResponse>builder()
                        .success(true)
                        .message("Driver fetched successfully")
                        .status(HttpStatus.OK.value())
                        .data(driver)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // List All Drivers (Paginated + Filtered)
    // ═══════════════════════════════════════════
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DriverResponse>>> getAllDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) DriverStatus status,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) DriverTier tier,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search
    ) {
        Page<DriverResponse> drivers = driverService.getAllDrivers(
                page, size, sortBy, sortDir,
                status, vehicleType, verificationStatus, tier, active, search
        );
        return ResponseEntity.ok(
                ApiResponse.<Page<DriverResponse>>builder()
                        .success(true)
                        .message("Drivers fetched successfully")
                        .status(HttpStatus.OK.value())
                        .data(drivers)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Update Driver Profile
    // ═══════════════════════════════════════════
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverRequest request
    ) {
        DriverResponse driver = driverService.updateDriver(id, request);
        return ResponseEntity.ok(
                ApiResponse.<DriverResponse>builder()
                        .success(true)
                        .message("Driver updated successfully")
                        .status(HttpStatus.OK.value())
                        .data(driver)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Soft Delete Driver
    // ═══════════════════════════════════════════
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDriver(@PathVariable Long id) {
        driverService.deleteDriver(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Driver deleted successfully")
                        .status(HttpStatus.OK.value())
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Update Driver Status (Online/Offline/Break)
    // ═══════════════════════════════════════════
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriverStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverStatusRequest request
    ) {
        DriverResponse driver = driverService.updateDriverStatus(id, request);
        return ResponseEntity.ok(
                ApiResponse.<DriverResponse>builder()
                        .success(true)
                        .message("Driver status updated to " + request.getStatus())
                        .status(HttpStatus.OK.value())
                        .data(driver)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Update Real-time Location
    // ═══════════════════════════════════════════
    @PatchMapping("/{id}/location")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriverLocation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDriverLocationRequest request
    ) {
        DriverResponse driver = driverService.updateDriverLocation(id, request);
        return ResponseEntity.ok(
                ApiResponse.<DriverResponse>builder()
                        .success(true)
                        .message("Location updated successfully")
                        .status(HttpStatus.OK.value())
                        .data(driver)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Verify / Reject Driver (Admin Only)
    // ═══════════════════════════════════════════
    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriverResponse>> verifyDriver(
            @PathVariable Long id,
            @RequestParam VerificationStatus status,
            @RequestParam(required = false) String rejectionReason
    ) {
        DriverResponse driver = driverService.verifyDriver(id, status, rejectionReason);
        return ResponseEntity.ok(
                ApiResponse.<DriverResponse>builder()
                        .success(true)
                        .message("Driver verification updated to " + status)
                        .status(HttpStatus.OK.value())
                        .data(driver)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Find Nearby Available Drivers
    // ═══════════════════════════════════════════
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<java.util.List<DriverResponse>>> findNearbyDrivers(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "5.0") Double radiusKm
    ) {
        java.util.List<DriverResponse> drivers = driverService.findNearbyDrivers(latitude, longitude, radiusKm);
        return ResponseEntity.ok(
                ApiResponse.<java.util.List<DriverResponse>>builder()
                        .success(true)
                        .message("Found " + drivers.size() + " nearby drivers")
                        .status(HttpStatus.OK.value())
                        .data(drivers)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Get My Profile (Driver self-service via JWT)
    // ═══════════════════════════════════════════
    @GetMapping("/me")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DriverResponse>> getMyProfile() {
        DriverResponse driver = driverService.getMyProfile();
        return ResponseEntity.ok(
                ApiResponse.<DriverResponse>builder()
                        .success(true)
                        .message("Profile fetched successfully")
                        .status(HttpStatus.OK.value())
                        .data(driver)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Update My Status (Driver self-service via JWT)
    // ═══════════════════════════════════════════
    @PutMapping("/me/status")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateMyStatus(
            @Valid @RequestBody UpdateDriverStatusRequest request
    ) {
        DriverResponse driver = driverService.updateMyStatus(request);
        return ResponseEntity.ok(
                ApiResponse.<DriverResponse>builder()
                        .success(true)
                        .message("Status updated to " + request.getStatus())
                        .status(HttpStatus.OK.value())
                        .data(driver)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Update My Location (Driver self-service via JWT)
    // ═══════════════════════════════════════════
    @PutMapping("/me/location")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateMyLocation(
            @Valid @RequestBody UpdateDriverLocationRequest request
    ) {
        DriverResponse driver = driverService.updateMyLocation(request);
        return ResponseEntity.ok(
                ApiResponse.<DriverResponse>builder()
                        .success(true)
                        .message("Location updated successfully")
                        .status(HttpStatus.OK.value())
                        .data(driver)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Driver Stats (Admin Dashboard)
    // ═══════════════════════════════════════════
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriverStatsResponse>> getDriverStats() {
        DriverStatsResponse stats = driverService.getDriverStats();
        return ResponseEntity.ok(
                ApiResponse.<DriverStatsResponse>builder()
                        .success(true)
                        .message("Driver stats fetched successfully")
                        .status(HttpStatus.OK.value())
                        .data(stats)
                        .build()
        );
    }
}

