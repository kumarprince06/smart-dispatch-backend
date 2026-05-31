package com.smartdispatch.analytics.service;

import com.smartdispatch.analytics.dto.DashboardStats;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.driver.enums.DriverStatus;
import com.smartdispatch.driver.repository.DriverRepository;
import com.smartdispatch.order.enums.OrderStatus;
import com.smartdispatch.order.repository.OrderRepository;
import com.smartdispatch.payment.enums.PaymentStatus;
import com.smartdispatch.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Analytics Engine.
 * Provides real-time dashboard data for admin panel.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    public DashboardStats getDashboardStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().minusDays(30).atStartOfDay();

        // Order counts
        Long totalOrders = orderRepository.count();
        Long todayOrders = orderRepository.countOrdersInRange(todayStart, todayEnd);
        Long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        Long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        Long pendingOrders = orderRepository.countByStatus(OrderStatus.REQUESTED);

        Long activeOrders = orderRepository.countByStatus(OrderStatus.ASSIGNED)
                + orderRepository.countByStatus(OrderStatus.PICKED_UP)
                + orderRepository.countByStatus(OrderStatus.IN_TRANSIT);

        // Revenue
        Double totalRevenue = orderRepository.getTotalRevenue();
        Double todayRevenue = orderRepository.getRevenueInRange(todayStart, todayEnd);
        Double weekRevenue = orderRepository.getRevenueInRange(weekStart, todayEnd);
        Double monthRevenue = orderRepository.getRevenueInRange(monthStart, todayEnd);
        Double avgOrderValue = orderRepository.getAverageOrderValue();

        // Performance
        Double avgDeliveryTime = orderRepository.getAverageDeliveryTimeMinutes();
        Double successRate = totalOrders > 0
                ? (deliveredOrders * 100.0) / totalOrders
                : 0.0;

        // Driver stats
        Long totalDrivers = driverRepository.count();
        Long onlineDrivers = driverRepository.countByStatus(DriverStatus.ONLINE)
                + driverRepository.countByStatus(DriverStatus.AVAILABLE);
        Long availableDrivers = driverRepository.countByStatus(DriverStatus.AVAILABLE);
        Long busyDrivers = driverRepository.countByStatus(DriverStatus.BUSY)
                + driverRepository.countByStatus(DriverStatus.ON_DELIVERY);

        // Customers
        Long totalCustomers = userRepository.count();

        // Payments
        Long totalPayments = paymentRepository.count();
        Long pendingPayments = paymentRepository.countByStatus(PaymentStatus.PENDING);
        Long failedPayments = paymentRepository.countByStatus(PaymentStatus.FAILED);

        // Peak hours (last 7 days)
        List<Map<String, Object>> peakHours = new ArrayList<>();
        try {
            List<Object[]> hourData = orderRepository.getOrdersByHour(weekStart);
            for (Object[] row : hourData) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("hour", row[0]);
                entry.put("count", row[1]);
                peakHours.add(entry);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch peak hours: {}", e.getMessage());
        }

        return DashboardStats.builder()
                .totalOrders(totalOrders)
                .todayOrders(todayOrders)
                .activeOrders(activeOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .pendingOrders(pendingOrders)
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .weekRevenue(weekRevenue)
                .monthRevenue(monthRevenue)
                .averageOrderValue(avgOrderValue != null ? Math.round(avgOrderValue * 100.0) / 100.0 : 0.0)
                .averageDeliveryTimeMinutes(avgDeliveryTime != null ? Math.round(avgDeliveryTime * 10.0) / 10.0 : 0.0)
                .deliverySuccessRate(Math.round(successRate * 10.0) / 10.0)
                .totalDrivers(totalDrivers)
                .onlineDrivers(onlineDrivers)
                .availableDrivers(availableDrivers)
                .busyDrivers(busyDrivers)
                .totalCustomers(totalCustomers)
                .totalPayments(totalPayments)
                .pendingPayments(pendingPayments)
                .failedPayments(failedPayments)
                .peakHours(peakHours)
                .build();
    }
}
