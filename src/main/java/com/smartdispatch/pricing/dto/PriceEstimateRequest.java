package com.smartdispatch.pricing.dto;

import com.smartdispatch.order.enums.OrderPriority;
import com.smartdispatch.order.enums.PackageType;
import lombok.Data;

@Data
public class PriceEstimateRequest {
    private Double pickupLat;
    private Double pickupLng;
    private Double dropoffLat;
    private Double dropoffLng;
    private OrderPriority priority;
    private PackageType packageType;
}
