package com.smartdispatch.pricing.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriceEstimateResponse {
    private Double distanceKm;
    private Double baseFee;
    private Double surgeMultiplier;
    private Double estimatedFee;
    private String currency;
}
