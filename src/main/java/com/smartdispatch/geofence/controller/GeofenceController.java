package com.smartdispatch.geofence.controller;

import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.geofence.entity.ServiceZone;
import com.smartdispatch.geofence.service.GeofenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/zones")
@RequiredArgsConstructor
public class GeofenceController {

    private final GeofenceService geofenceService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceZone>> createZone(@RequestBody ServiceZone zone) {
        ServiceZone created = geofenceService.createZone(zone);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ServiceZone>builder()
                .success(true).message("Service zone created").status(201).data(created).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceZone>>> getAllZones() {
        return ResponseEntity.ok(ApiResponse.<List<ServiceZone>>builder()
                .success(true).message("Zones fetched").status(200)
                .data(geofenceService.getAllZones()).build());
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<ServiceZone>> checkLocation(
            @RequestParam Double lat, @RequestParam Double lng
    ) {
        ServiceZone zone = geofenceService.validateLocation(lat, lng);
        return ResponseEntity.ok(ApiResponse.<ServiceZone>builder()
                .success(true).message("Location is within zone: " + zone.getName())
                .status(200).data(zone).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteZone(@PathVariable Long id) {
        geofenceService.deleteZone(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Zone deleted").status(200).build());
    }
}
