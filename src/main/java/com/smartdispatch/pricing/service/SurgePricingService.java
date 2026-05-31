package com.smartdispatch.pricing.service;

import com.smartdispatch.driver.enums.DriverStatus;
import com.smartdispatch.driver.repository.DriverRepository;
import com.smartdispatch.order.enums.OrderStatus;
import com.smartdispatch.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Surge Pricing Engine.
 * Calculates multiplier based on demand (active orders) / supply (available drivers).
 * Similar to Uber's surge pricing algorithm.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SurgePricingService {

    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;

    private static final double MIN_MULTIPLIER = 1.0;
    private static final double MAX_MULTIPLIER = 3.0;

    /**
     * Calculate surge multiplier.
     * Formula: demand / supply ratio mapped to 1.0 - 3.0 range.
     */
    public double getSurgeMultiplier() {
        // Demand: orders waiting for assignment
        long demand = orderRepository.countByStatus(OrderStatus.REQUESTED);

        // Supply: available drivers
        long supply = driverRepository.countByStatus(DriverStatus.AVAILABLE);

        if (supply == 0) {
            log.warn("Surge: No drivers available. Max multiplier applied.");
            return MAX_MULTIPLIER;
        }

        double ratio = (double) demand / supply;

        // Map ratio to multiplier
        double multiplier;
        if (ratio <= 0.5) {
            multiplier = 1.0;       // Low demand → no surge
        } else if (ratio <= 1.0) {
            multiplier = 1.2;       // Normal
        } else if (ratio <= 2.0) {
            multiplier = 1.5;       // High demand
        } else if (ratio <= 3.0) {
            multiplier = 2.0;       // Very high demand
        } else {
            multiplier = 2.5;       // Peak demand
        }

        multiplier = Math.min(multiplier, MAX_MULTIPLIER);

        log.info("Surge: demand={}, supply={}, ratio={}, multiplier={}x",
                demand, supply, ratio, multiplier);

        return multiplier;
    }

    /**
     * Apply surge to base fee.
     */
    public double applySerge(double baseFee) {
        double multiplier = getSurgeMultiplier();
        double surgedFee = baseFee * multiplier;
        return Math.round(surgedFee * 100.0) / 100.0;
    }

    /**
     * Get surge details (for UI display).
     */
    public SurgeInfo getSurgeInfo() {
        long demand = orderRepository.countByStatus(OrderStatus.REQUESTED);
        long supply = driverRepository.countByStatus(DriverStatus.AVAILABLE);
        double multiplier = getSurgeMultiplier();

        return new SurgeInfo(demand, supply, multiplier, multiplier > 1.0);
    }

    public record SurgeInfo(long demand, long supply, double multiplier, boolean surgeActive) {}
}
