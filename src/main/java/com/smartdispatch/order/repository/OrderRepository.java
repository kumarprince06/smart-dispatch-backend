package com.smartdispatch.order.repository;

import com.smartdispatch.order.entity.Order;
import com.smartdispatch.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByTrackingNumber(String trackingNumber);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByCustomerEmail(String email, Pageable pageable);

    Page<Order> findByDriverUserEmail(String email, Pageable pageable);

    Page<Order> findByCustomerEmailAndStatus(String email, OrderStatus status, Pageable pageable);

    Page<Order> findByDriverUserEmailAndStatus(String email, OrderStatus status, Pageable pageable);

    // Filtered listing with search
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
}
