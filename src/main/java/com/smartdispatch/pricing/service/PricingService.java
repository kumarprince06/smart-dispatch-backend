package com.smartdispatch.pricing.service;

import com.smartdispatch.order.enums.OrderPriority;
import com.smartdispatch.order.enums.PackageType;
import org.springframework.stereotype.Service;

/**
 * Service responsible for all pricing and distance calculations.
 * Extracted to follow the Single Responsibility Principle (SOLID).
 */
@Service
public class PricingService {

    private static final double BASE_FEE = 30.0;
    private static final double PER_KM_RATE = 12.0;

    /**
     * Calculates the delivery fee based on distance, priority, and package type.
     */
    public double calculateFee(double distanceKm, OrderPriority priority, PackageType packageType) {
        double fee = BASE_FEE + (PER_KM_RATE * distanceKm);

        // Priority multiplier
        if (priority != null) {
            switch (priority) {
                case EXPRESS -> fee *= 1.5;
                case URGENT -> fee *= 2.0;
                default -> {} // STANDARD = 1x
            }
        }

        // Package type surcharge
        if (packageType != null) {
            switch (packageType) {
                case FRAGILE -> fee += 50.0;
                case FURNITURE -> fee += 100.0;
                case MEDICAL -> fee += 30.0;
                case ELECTRONICS -> fee += 40.0;
                default -> {}
            }
        }

        return Math.round(fee * 100.0) / 100.0;
    }

    /**
     * Calculates Haversine distance in kilometers between two points.
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
