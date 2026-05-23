package com.smartdispatch.order.repository;

import com.smartdispatch.order.entity.Order;
import com.smartdispatch.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"customer", "driver"})
    Optional<Order> findByTrackingNumber(String trackingNumber);

    @EntityGraph(attributePaths = {"customer", "driver"})
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "driver"})
    Page<Order> findByCustomerEmail(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "driver"})
    Page<Order> findByDriverUserEmail(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "driver"})
    Page<Order> findByCustomerEmailAndStatus(String email, OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "driver"})
    Page<Order> findByDriverUserEmailAndStatus(String email, OrderStatus status, Pageable pageable);

    // Filtered listing with search
    @EntityGraph(attributePaths = {"customer", "driver"})
    @Query("SELECT o FROM Order o WHERE " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:search IS NULL OR LOWER(o.trackingNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(o.pickupAddress) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(o.dropAddress) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> findAllWithFilters(
            @Param("status") OrderStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    // Count by status
    Long countByStatus(OrderStatus status);

    // Count by customer
    Long countByCustomerEmail(String email);

    // Active orders for a driver
    @Query("SELECT COUNT(o) FROM Order o WHERE o.driver.id = :driverId AND o.status IN ('ASSIGNED', 'PICKED_UP', 'IN_TRANSIT')")
    Long countActiveOrdersByDriverId(@Param("driverId") Long driverId);

    // Scheduled orders that are due for dispatch
    @Query("SELECT o FROM Order o WHERE o.isScheduled = true AND o.status = 'CREATED' AND o.scheduledAt <= :now")
    List<Order> findScheduledOrdersDue(@Param("now") LocalDateTime now);

    // ═══════════════════════════════════════════
    // Analytics Queries
    // ═══════════════════════════════════════════

    // Total revenue
    @Query("SELECT COALESCE(SUM(o.deliveryFee), 0) FROM Order o WHERE o.status = 'DELIVERED'")
    Double getTotalRevenue();

    // Revenue in date range
    @Query("SELECT COALESCE(SUM(o.deliveryFee), 0) FROM Order o WHERE o.status = 'DELIVERED' AND o.deliveredAt BETWEEN :start AND :end")
    Double getRevenueInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Orders in date range
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    Long countOrdersInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Average delivery time (minutes)
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (delivered_at - assigned_at)) / 60) FROM orders WHERE status = 'DELIVERED' AND delivered_at IS NOT NULL AND assigned_at IS NOT NULL", nativeQuery = true)
    Double getAverageDeliveryTimeMinutes();

    // Average order value
    @Query("SELECT AVG(o.deliveryFee) FROM Order o WHERE o.status = 'DELIVERED'")
    Double getAverageOrderValue();

    // Orders per hour (for peak hour analysis)
    @Query(value = "SELECT EXTRACT(HOUR FROM created_at) as hour, COUNT(*) as count FROM orders WHERE created_at >= :start GROUP BY hour ORDER BY count DESC", nativeQuery = true)
    List<Object[]> getOrdersByHour(@Param("start") LocalDateTime start);
}
