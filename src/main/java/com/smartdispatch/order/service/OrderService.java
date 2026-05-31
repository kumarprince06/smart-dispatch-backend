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
import com.smartdispatch.geofence.service.GeofenceService;
import com.smartdispatch.pricing.service.PricingService;
import com.smartdispatch.order.dto.*;
import com.smartdispatch.order.entity.Order;
import com.smartdispatch.order.entity.OrderTimeline;
import com.smartdispatch.order.enums.OrderPriority;
import com.smartdispatch.order.enums.OrderStatus;
import com.smartdispatch.order.enums.PackageType;
import com.smartdispatch.order.mapper.OrderMapper;
import com.smartdispatch.kafka.event.OrderEvent;
import com.smartdispatch.kafka.producer.OrderEventProducer;
import com.smartdispatch.notification.service.NotificationService;
import com.smartdispatch.notification.enums.NotificationTemplate;
import com.smartdispatch.notification.enums.NotificationType;
import com.smartdispatch.order.repository.OrderRepository;
import com.smartdispatch.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final OrderEventProducer orderEventProducer;
    private final GeofenceService geofenceService;
    private final PricingService pricingService;
    private final NotificationService notificationService;
    private final OrderMapper orderMapper;

    // Valid state transitions (State Machine)
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.REQUESTED, Set.of(OrderStatus.ASSIGNED, OrderStatus.CANCELLED),
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
    @CacheEvict(value = "order-stats", allEntries = true)
    public OrderResponse createOrder(CreateOrderRequest request) {

        String email = SecurityUtil.getCurrentUserEmail();
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Customer not found"));

        // Validate Geofence Zones (Ensure pickup & drop are in service areas)
        geofenceService.validateOrderLocations(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropLatitude(), request.getDropLongitude()
        );

        // Get Zone-based surge multiplier
        Double pickupMultiplier = geofenceService.getZoneMultiplier(request.getPickupLatitude(), request.getPickupLongitude());
        Double dropMultiplier = geofenceService.getZoneMultiplier(request.getDropLatitude(), request.getDropLongitude());
        Double activeSurgeMultiplier = Math.max(pickupMultiplier, dropMultiplier); // Take the higher surge

        // Calculate distance
        double distanceKm = pricingService.calculateDistance(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropLatitude(), request.getDropLongitude()
        );

        // Calculate fee (Base * Surge)
        OrderPriority priority = request.getPriority() != null ? request.getPriority() : OrderPriority.STANDARD;
        double baseFee = pricingService.calculateFee(distanceKm, priority, request.getPackageType());
        double deliveryFee = baseFee * activeSurgeMultiplier;

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
                .status(OrderStatus.REQUESTED)
                .isScheduled(request.getScheduledAt() != null && !request.getScheduledAt().trim().isEmpty())
                .scheduledAt(request.getScheduledAt() != null && !request.getScheduledAt().trim().isEmpty() 
                        ? LocalDateTime.parse(request.getScheduledAt()) : null)
                .surgeMultiplier(activeSurgeMultiplier)
                .build();

        // Save order items natively in database
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            java.util.List<com.smartdispatch.order.entity.OrderItem> orderItems = request.getItems().stream()
                    .map(itemReq -> com.smartdispatch.order.entity.OrderItem.builder()
                            .order(order)
                            .name(itemReq.getName())
                            .weightKg(itemReq.getWeightKg())
                            .lengthCm(itemReq.getLengthCm())
                            .widthCm(itemReq.getWidthCm())
                            .heightCm(itemReq.getHeightCm())
                            .quantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1)
                            .build())
                    .collect(java.util.stream.Collectors.toList());
            order.setItems(orderItems);
        }

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

        // Send Push Notification if assigned
        if (savedOrder.getStatus() == OrderStatus.ASSIGNED && savedOrder.getDriver() != null) {
            String fcmToken = savedOrder.getDriver().getUser().getFcmToken() != null 
                              ? savedOrder.getDriver().getUser().getFcmToken() 
                              : savedOrder.getDriver().getUser().getId().toString();
            notificationService.sendNotification(
                    savedOrder.getDriver().getUser().getId(),
                    fcmToken,
                    NotificationTemplate.DRIVER_ASSIGNED_NEW_ORDER,
                    NotificationType.PUSH,
                    "/orders/" + savedOrder.getId(),
                    savedOrder.getTrackingNumber(),
                    savedOrder.getPickupAddress()
            );
        }

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
        
        // Prevent Postgres bytea lower() error by passing empty string instead of null
        String safeSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : "";
        
        return orderRepository.findAllWithFilters(status, safeSearch, pageable).map(orderMapper::toResponse);
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
    // Manually Assign Driver (Admin)
    // ═══════════════════════════════════════════
    @Transactional
    public OrderResponse manuallyAssignDriver(Long orderId, AssignDriverRequest request) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.REQUESTED) {
            throw new BadRequestException("Only REQUESTED orders can be manually assigned. Current status: " + order.getStatus());
        }

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new BadRequestException("Driver not found"));
        
        order.setDriver(driver);
        order.setStatus(OrderStatus.ASSIGNED);
        order.setAssignedAt(LocalDateTime.now());

        // Update driver state
        driverService.incrementActiveOrders(request.getDriverId());

        Order savedOrder = orderRepository.save(order);

        // Add timeline entry
        addTimelineEntry(savedOrder, OrderStatus.ASSIGNED, "Driver assigned manually by Admin", SecurityUtil.getCurrentUserEmail());

        log.info("Order {} manually assigned to driver {}", order.getTrackingNumber(), request.getDriverId());

        // Publish events
        publishOrderEvent(savedOrder);

        // Send Push Notification
        String fcmToken = driver.getUser().getFcmToken() != null 
                          ? driver.getUser().getFcmToken() 
                          : driver.getUser().getId().toString();
        notificationService.sendNotification(
                driver.getUser().getId(),
                fcmToken,
                NotificationTemplate.DRIVER_ASSIGNED_NEW_ORDER,
                NotificationType.PUSH,
                "/orders/" + savedOrder.getId(),
                savedOrder.getTrackingNumber(),
                savedOrder.getPickupAddress()
        );

        return orderMapper.toResponse(savedOrder);
    }

    // ═══════════════════════════════════════════
    // Update Order Status (State Machine)
    // ═══════════════════════════════════════════
    @Transactional
    @CacheEvict(value = "order-stats", allEntries = true)
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
            if (request.getProofOfDeliveryUrl() != null) {
                order.setProofOfDeliveryUrl(request.getProofOfDeliveryUrl());
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

        // 🔴 REAL-TIME: Publish event to Kafka
        publishOrderEvent(updated);

        return orderMapper.toResponse(updated);
    }

    // ═══════════════════════════════════════════
    // Cancel Order
    // ═══════════════════════════════════════════
    @Transactional
    @CacheEvict(value = "order-stats", allEntries = true)
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
        log.info("Loading order stats from database");
        return OrderStatsResponse.builder()
                .totalOrders(orderRepository.count())
                .createdOrders(orderRepository.countByStatus(OrderStatus.REQUESTED))
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

    // Publish order status change to Kafka Event Bus
    private void publishOrderEvent(Order order) {
        String humanMessage = switch (order.getStatus()) {
            case REQUESTED -> "Your order has been placed!";
            case PAYMENT_PENDING -> "Awaiting payment for your order.";
            case PAYMENT_FAILED -> "Payment failed for your order.";
            case CONFIRMED -> "Your order is confirmed!";
            case ASSIGNED -> "A driver has been assigned to your order";
            case PICKED_UP -> "Your package has been picked up!";
            case IN_TRANSIT -> "Your package is on the way!";
            case DELIVERED -> "Your package has been delivered!";
            case CANCELLED -> "Your order has been cancelled";
            case FAILED -> "Delivery failed. We'll retry soon.";
        };

        OrderEvent.OrderEventBuilder builder = OrderEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .orderId(order.getId())
                .trackingNumber(order.getTrackingNumber())
                .status(order.getStatus())
                .customerEmail(order.getCustomer() != null ? order.getCustomer().getEmail() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getFirstName() : null)
                .timestamp(LocalDateTime.now())
                .message(humanMessage);

        if (order.getDriver() != null) {
            builder.driverName(order.getDriver().getUser().getFirstName() + " " + order.getDriver().getUser().getLastName())
                    .vehicleNumber(order.getDriver().getVehicleNumber())
                    .driverLatitude(order.getDriver().getCurrentLatitude())
                    .driverLongitude(order.getDriver().getCurrentLongitude());
        }

        orderEventProducer.publishOrderEvent(builder.build());
    }
}

