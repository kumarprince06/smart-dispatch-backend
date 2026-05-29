package com.smartdispatch.analytics.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    // Order Stats
    private Long totalOrders;
    private Long todayOrders;
    private Long activeOrders; // ASSIGNED + PICKED_UP + IN_TRANSIT
    private Long deliveredOrders;
    private Long cancelledOrders;
    private Long pendingOrders; // CREATED (unassigned)

    // Revenue
    private Double totalRevenue;
    private Double todayRevenue;
    private Double weekRevenue;
    private Double monthRevenue;
    private Double averageOrderValue;

    // Performance
    private Double averageDeliveryTimeMinutes;
    private Double deliverySuccessRate;

    // Driver Stats
    private Long totalDrivers;
    private Long onlineDrivers;
    private Long availableDrivers;
    private Long busyDrivers;

    // Customer Stats
    private Long totalCustomers;

    // Payment Stats
    private Long totalPayments;
    private Long pendingPayments;
    private Long failedPayments;

    // Peak Hours (hour → order count)
    private List<Map<String, Object>> peakHours;
}
