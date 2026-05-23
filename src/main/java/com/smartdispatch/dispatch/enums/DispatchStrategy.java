package com.smartdispatch.dispatch.enums;

/**
 * Dispatch strategy determines how the system selects a driver.
 */
public enum DispatchStrategy {
    NEAREST,        // Closest driver by distance
    LEAST_BUSY,     // Driver with fewest active orders
    HIGHEST_RATED,  // Best-rated driver
    ROUND_ROBIN     // Fair distribution
}
