package com.smartdispatch.pricing.dto;

import com.smartdispatch.order.enums.OrderPriority;
import com.smartdispatch.order.enums.PackageType;
import lombok.Data;
import java.util.List;

@Data
public class PriceEstimateRequest {
    private Double pickupLat;
    private Double pickupLng;
    private Double dropoffLat;
    private Double dropoffLng;
    private OrderPriority priority;
    private PackageType packageType;
    private List<PackageItem> items;

    @Data
    public static class PackageItem {
        private Double weightKg;
        private Double lengthCm;
        private Double widthCm;
        private Double heightCm;
        private Integer quantity;
    }
}
