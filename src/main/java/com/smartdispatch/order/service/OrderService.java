package com.smartdispatch.order.service;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.dispatch.dto.DispatchResult;
import com.smartdispatch.dispatch.enums.DispatchStrategy;
import com.smartdispatch.dispatch.service.DispatchService;
import com.smartdispatch.driver.entity.Driver;
import com.smartdispatch.driver.enums.DriverStatus;
import com.smartdispatch.driver.repository.DriverRepository;
import com.smartdispatch.driver.service.DriverService;
import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.order.dto.*;
import com.smartdispatch.order.entity.Order;
import com.smartdispatch.order.entity.OrderTimeline;
import com.smartdispatch.order.enums.OrderPriority;
import com.smartdispatch.order.enums.OrderStatus;
import com.smartdispatch.order.enums.PackageType;
import com.smartdispatch.order.mapper.OrderMapper;
import com.smartdispatch.order.repository.OrderRepository;
import com.smartdispatch.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final DriverService driverService;
    private final DispatchService dispatchService;
    private final OrderMapper orderMapper;

    // Valid state transitions (State Machine)
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.ASSIGNED, OrderStatus.CANCELLED),
            OrderStatus.ASSIGNED, Set.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED),
            OrderStatus.PICKED_UP, Set.of(OrderStatus.IN_TRANSIT, OrderStatus.CANCELLED),
            OrderStatus.IN_TRANSIT, Set.of(OrderStatus.DELIVERED, OrderStatus.FAILED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of(),
            OrderStatus.FAILED, Set.of(OrderStatus.ASSIGNED)
    );

    // ═══════════════════════════════════════════
    // Create Order
    // ═══════════════════════════════════════════
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        String email = SecurityUtil.getCurrentUserEmail();
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Customer not found"));

        // Calculate distance
        double distanceKm = calculateDistance(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropLatitude(), request.getDropLongitude()
        );

        // Calculate fee
        OrderPriority priority = request.getPriority() != null ? request.getPriority() : OrderPriority.STANDARD;
        double deliveryFee = calculateFee(distanceKm, priority, request.getPackageType());

        // Generate OTPs
        String pickupOtp = generateOtp();
        String deliveryOtp = generateOtp();

        // Build order
        Order order = Order.builder()
                .customer(customer)
                .pickupAddress(request.getPickupAddress())
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .pickupContactName(request.getPickupContactName())
                .pickupContactPhone(request.getPickupContactPhone())
                .dropAddress(request.getDropAddress())
                .dropLatitude(request.getDropLatitude())
                .dropLongitude(request.getDropLongitude())
                .dropContactName(request.getDropContactName())
                .dropContactPhone(request.getDropContactPhone())
                .packageType(request.getPackageType() != null ? request.getPackageType() : PackageType.SMALL_PARCEL)
                .packageDescription(request.getPackageDescription())
                .packageWeightKg(request.getPackageWeightKg())
                .priority(priority)
                .deliveryFee(deliveryFee)
                .distanceKm(Math.round(distanceKm * 100.0) / 100.0)
                .pickupOtp(pickupOtp)
                .deliveryOtp(deliveryOtp)
                .customerNotes(request.getCustomerNotes())
                .status(OrderStatus.CREATED)
                .build();

        // Try auto-assign driver via Redis GEO Dispatch Engine
        try {
            DispatchResult result = dispatchService.findNearestDriver(
                    request.getPickupLatitude(), request.getPickupLongitude(),
                    DispatchStrategy.NEAREST
            );

            Driver driver = driverRepository.findById(result.getDriverId())
                    .orElseThrow(() -> new BadRequestException("Driver not found"));

            order.setDriver(driver);
            order.setStatus(OrderStatus.ASSIGNED);
            order.setAssignedAt(LocalDateTime.now());
            order.setEstimatedDeliveryAt(LocalDateTime.now().plusMinutes(result.getEstimatedMinutes()));

            // Update driver state
            driverService.incrementActiveOrders(driver.getId());

            log.info("Order auto-assigned via dispatch engine. Driver: {}, Distance: {}km, Time: {}ms",
                    result.getDriverId(), result.getDistanceKm(), result.getDispatchTimeMs());
        } catch (BadRequestException e) {
            // No driver available — order stays CREATED
            log.warn("No available driver for order. Status remains CREATED.");
        }

        Order savedOrder = orderRepository.save(order);

        // Add timeline entry
        addTimelineEntry(savedOrder, savedOrder.getStatus(), "Order created", customer.getEmail());

        log.info("Order created. ID: {}, Tracking: {}", savedOrder.getId(), savedOrder.getTrackingNumber());

        return orderMapper.toResponse(savedOrder);
    }

    // ═══════════════════════════════════════════
    // Get Order by ID
    // ═══════════════════════════════════════════
    public OrderResponse getOrderById(Long id) {
        Order order = findOrderOrThrow(id);
        return orderMapper.toResponse(order);
    }

    // ═══════════════════════════════════════════
    // Get Order by Tracking Number
    // ═══════════════════════════════════════════
    public OrderResponse getOrderByTrackingNumber(String trackingNumber) {
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new BadRequestException("Order not found: " + trackingNumber));
        return orderMapper.toResponse(order);
    }

    // ═══════════════════════════════════════════
    // List All Orders (Admin — Paginated + Filtered)
    // ═══════════════════════════════════════════
    public Page<OrderResponse> getAllOrders(int page, int size, String sortBy, String sortDir,
                                            OrderStatus status, String search) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return orderRepository.findAllWithFilters(status, search, pageable).map(orderMapper::toResponse);
    }

    // ═══════════════════════════════════════════
    // Get My Orders (Customer)
    // ═══════════════════════════════════════════
    public Page<OrderResponse> getMyOrders(int page, int size, OrderStatus status) {
        String email = SecurityUtil.getCurrentUserEmail();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (status != null) {
            return orderRepository.findByCustomerEmailAndStatus(email, status, pageable).map(orderMapper::toResponse);
        }
        return orderRepository.findByCustomerEmail(email, pageable).map(orderMapper::toResponse);
    }

    // ═══════════════════════════════════════════
    // Get Driver Orders
    // ═══════════════════════════════════════════
    public Page<OrderResponse> getDriverOrders(int page, int size, OrderStatus status) {
        String email = SecurityUtil.getCurrentUserEmail();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (status != null) {
            return orderRepository.findByDriverUserEmailAndStatus(email, status, pageable).map(orderMapper::toResponse);
        }
        return orderRepository.findByDriverUserEmail(email, pageable).map(orderMapper::toResponse);
    }

    // ═══════════════════════════════════════════
    // Update Order Status (State Machine)
    // ═══════════════════════════════════════════
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = findOrderOrThrow(orderId);

        // Validate state transition
        validateTransition(order.getStatus(), request.getStatus());

        // OTP verification for PICKED_UP
        if (request.getStatus() == OrderStatus.PICKED_UP) {
            if (request.getOtp() == null || !request.getOtp().equals(order.getPickupOtp())) {
                throw new BadRequestException("Invalid pickup OTP");
            }
            order.setPickedUpAt(LocalDateTime.now());
        }

        // OTP verification for DELIVERED
        if (request.getStatus() == OrderStatus.DELIVERED) {
            if (request.getOtp() == null || !request.getOtp().equals(order.getDeliveryOtp())) {
                throw new BadRequestException("Invalid delivery OTP");
            }
            order.setDeliveredAt(LocalDateTime.now());

            // Release driver
            if (order.getDriver() != null) {
                driverService.decrementActiveOrders(order.getDriver().getId());
                order.getDriver().setTotalDeliveries(order.getDriver().getTotalDeliveries() + 1);
                order.getDriver().setTotalTrips(order.getDriver().getTotalTrips() + 1);
            }
        }

        if (request.getStatus() == OrderStatus.IN_TRANSIT) {
            order.setInTransitAt(LocalDateTime.now());
        }

        order.setStatus(request.getStatus());
        Order updated = orderRepository.save(order);

        // Add timeline
        String updatedBy = SecurityUtil.getCurrentUserEmail();
        addTimelineEntry(updated, request.getStatus(), "Status updated to " + request.getStatus(), updatedBy);

        log.info("Order {} status updated to {}", orderId, request.getStatus());

        return orderMapper.toResponse(updated);
    }

    // ═══════════════════════════════════════════
    // Cancel Order
    // ═══════════════════════════════════════════
    @Transactional
    public void cancelOrder(Long orderId, CancelOrderRequest request) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Delivered order cannot be cancelled");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.getReason());
        order.setCancelledBy(SecurityUtil.getCurrentUserEmail());
        order.setCancelledAt(LocalDateTime.now());

        // Release driver
        if (order.getDriver() != null) {
            driverService.decrementActiveOrders(order.getDriver().getId());
        }

        orderRepository.save(order);
        addTimelineEntry(order, OrderStatus.CANCELLED, "Cancelled: " + request.getReason(), SecurityUtil.getCurrentUserEmail());

        log.info("Order {} cancelled. Reason: {}", orderId, request.getReason());
    }

    // ═══════════════════════════════════════════
    // Rate Order (Customer)
    // ═══════════════════════════════════════════
    @Transactional
    public OrderResponse rateOrder(Long orderId, RateOrderRequest request) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Only delivered orders can be rated");
        }
        if (order.getCustomerRating() != null) {
            throw new BadRequestException("Order has already been rated");
        }

        order.setCustomerRating(request.getRating());
        order.setCustomerFeedback(request.getFeedback());
        Order updated = orderRepository.save(order);

        // Update driver rating
        if (order.getDriver() != null) {
            driverService.updateDriverRating(order.getDriver().getId(), request.getRating());
        }

        log.info("Order {} rated: {} stars", orderId, request.getRating());

        return orderMapper.toResponse(updated);
    }

    // ═══════════════════════════════════════════
    // Order Stats (Admin)
    // ═══════════════════════════════════════════
    public OrderStatsResponse getOrderStats() {
        return OrderStatsResponse.builder()
                .totalOrders(orderRepository.count())
                .createdOrders(orderRepository.countByStatus(OrderStatus.CREATED))
                .assignedOrders(orderRepository.countByStatus(OrderStatus.ASSIGNED))
                .pickedUpOrders(orderRepository.countByStatus(OrderStatus.PICKED_UP))
                .inTransitOrders(orderRepository.countByStatus(OrderStatus.IN_TRANSIT))
                .deliveredOrders(orderRepository.countByStatus(OrderStatus.DELIVERED))
                .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))
                .failedOrders(orderRepository.countByStatus(OrderStatus.FAILED))
                .build();
    }

    // ═══════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Order not found with ID: " + id));
    }

    // State machine validation
    private void validateTransition(OrderStatus current, OrderStatus next) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(next)) {
            throw new BadRequestException(
                    "Invalid status transition: " + current + " → " + next
            );
        }
    }


    // Haversine distance calculation (km)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Dynamic pricing: base + per km + priority multiplier
    private double calculateFee(double distanceKm, OrderPriority priority, PackageType packageType) {
        double baseFee = 30.0;
        double perKmRate = 12.0;
        double fee = baseFee + (perKmRate * distanceKm);

        // Priority multiplier
        switch (priority) {
            case EXPRESS -> fee *= 1.5;
            case URGENT -> fee *= 2.0;
            default -> {} // STANDARD = 1x
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

    // Generate 4-digit OTP
    private String generateOtp() {
        return String.format("%04d", new Random().nextInt(10000));
    }

    // Add timeline entry
    private void addTimelineEntry(Order order, OrderStatus status, String description, String updatedBy) {
        OrderTimeline timeline = OrderTimeline.builder()
                .order(order)
                .status(status)
                .description(description)
                .updatedBy(updatedBy)
                .timestamp(LocalDateTime.now())
                .build();
        order.getTimeline().add(timeline);
        orderRepository.save(order);
    }
}
