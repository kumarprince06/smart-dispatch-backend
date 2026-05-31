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

        // Calculate Volumetric Weight Surcharge for multiple items
        double totalChargeableWeight = 0.0;
        
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (PriceEstimateRequest.PackageItem item : request.getItems()) {
                int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
                double weight = item.getWeightKg() != null ? item.getWeightKg() : 1.0;
                double l = item.getLengthCm() != null ? item.getLengthCm() : 0.0;
                double w = item.getWidthCm() != null ? item.getWidthCm() : 0.0;
                double h = item.getHeightCm() != null ? item.getHeightCm() : 0.0;

                double volWeight = (l * w * h) / 5000.0;
                totalChargeableWeight += Math.max(weight, volWeight) * qty;
            }
        } else {
            totalChargeableWeight = 1.0;
        }

        // Add ₹10 for every extra kg over 1kg
        if (totalChargeableWeight > 1.0) {
            baseFee += Math.ceil(totalChargeableWeight - 1.0) * 10.0;
        }

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
