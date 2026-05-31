package com.smartdispatch.pricing.controller;

import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.pricing.dto.PriceEstimateRequest;
import com.smartdispatch.pricing.dto.PriceEstimateResponse;
import com.smartdispatch.pricing.service.PricingService;
import com.smartdispatch.pricing.service.SurgePricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;
    private final SurgePricingService surgePricingService;

    @PostMapping("/estimate")
    public ResponseEntity<ApiResponse<PriceEstimateResponse>> getEstimate(@RequestBody PriceEstimateRequest request) {
        if (request.getPickupLat() == null || request.getPickupLng() == null || 
            request.getDropoffLat() == null || request.getDropoffLng() == null) {
            throw new BadRequestException("Pickup and Dropoff coordinates are required");
        }

        // 1. Calculate Distance
        double distanceKm = pricingService.calculateDistance(
                request.getPickupLat(), request.getPickupLng(),
                request.getDropoffLat(), request.getDropoffLng()
        );

        // 2. Calculate Base Fee
        double baseFee = pricingService.calculateFee(distanceKm, request.getPriority(), request.getPackageType());

        // 3. Apply Surge Multiplier
        double surgeMultiplier = surgePricingService.getSurgeMultiplier();
        double finalFee = Math.round((baseFee * surgeMultiplier) * 100.0) / 100.0;

        PriceEstimateResponse response = PriceEstimateResponse.builder()
                .distanceKm(Math.round(distanceKm * 10.0) / 10.0)
                .baseFee(baseFee)
                .surgeMultiplier(surgeMultiplier)
                .estimatedFee(finalFee)
                .currency("₹")
                .build();

        return ResponseEntity.ok(ApiResponse.<PriceEstimateResponse>builder()
                .success(true)
                .message("Price estimate calculated successfully")
                .data(response)
                .build());
    }
}
