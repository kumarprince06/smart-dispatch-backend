package com.smartdispatch.order.controller;

import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.order.dto.*;
import com.smartdispatch.order.enums.OrderStatus;
import com.smartdispatch.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ═══════════════════════════════════════════
    // Create Order (Customer)
    // ═══════════════════════════════════════════
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<OrderResponse>builder()
                        .success(true)
                        .message("Order created successfully")
                        .status(HttpStatus.CREATED.value())
                        .data(order)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Get Order by ID
    // ═══════════════════════════════════════════
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .success(true)
                        .message("Order fetched successfully")
                        .status(HttpStatus.OK.value())
                        .data(order)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Track Order by Tracking Number (Public-friendly)
    // ═══════════════════════════════════════════
    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> trackOrder(@PathVariable String trackingNumber) {
        OrderResponse order = orderService.getOrderByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .success(true)
                        .message("Order tracked successfully")
                        .status(HttpStatus.OK.value())
                        .data(order)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // List All Orders (Admin — Paginated + Filtered)
    // ═══════════════════════════════════════════
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search
    ) {
        Page<OrderResponse> orders = orderService.getAllOrders(page, size, sortBy, sortDir, status, search);
        return ResponseEntity.ok(
                ApiResponse.<Page<OrderResponse>>builder()
                        .success(true)
                        .message("Orders fetched successfully")
                        .status(HttpStatus.OK.value())
                        .data(orders)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Get My Orders (Customer)
    // ═══════════════════════════════════════════
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status
    ) {
        Page<OrderResponse> orders = orderService.getMyOrders(page, size, status);
        return ResponseEntity.ok(
                ApiResponse.<Page<OrderResponse>>builder()
                        .success(true)
                        .message("Orders fetched successfully")
                        .status(HttpStatus.OK.value())
                        .data(orders)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Get Driver Orders
    // ═══════════════════════════════════════════
    @GetMapping("/driver-orders")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getDriverOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status
    ) {
        Page<OrderResponse> orders = orderService.getDriverOrders(page, size, status);
        return ResponseEntity.ok(
                ApiResponse.<Page<OrderResponse>>builder()
                        .success(true)
                        .message("Driver orders fetched successfully")
                        .status(HttpStatus.OK.value())
                        .data(orders)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Update Order Status (State Machine)
    // ═══════════════════════════════════════════
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .success(true)
                        .message("Order status updated to " + request.getStatus())
                        .status(HttpStatus.OK.value())
                        .data(order)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Cancel Order
    // ═══════════════════════════════════════════
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        orderService.cancelOrder(id, request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Order cancelled successfully")
                        .status(HttpStatus.OK.value())
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Manually Assign Driver (Admin)
    // ═══════════════════════════════════════════
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> manuallyAssignDriver(
            @PathVariable Long id,
            @Valid @RequestBody AssignDriverRequest request
    ) {
        OrderResponse order = orderService.manuallyAssignDriver(id, request);
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .success(true)
                        .message("Driver manually assigned to order")
                        .status(HttpStatus.OK.value())
                        .data(order)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Rate Order (Customer)
    // ═══════════════════════════════════════════
    @PostMapping("/{id}/rate")
    public ResponseEntity<ApiResponse<OrderResponse>> rateOrder(
            @PathVariable Long id,
            @Valid @RequestBody RateOrderRequest request
    ) {
        OrderResponse order = orderService.rateOrder(id, request);
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .success(true)
                        .message("Order rated successfully")
                        .status(HttpStatus.OK.value())
                        .data(order)
                        .build()
        );
    }

    // ═══════════════════════════════════════════
    // Order Stats (Admin)
    // ═══════════════════════════════════════════
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderStatsResponse>> getOrderStats() {
        OrderStatsResponse stats = orderService.getOrderStats();
        return ResponseEntity.ok(
                ApiResponse.<OrderStatsResponse>builder()
                        .success(true)
                        .message("Order stats fetched successfully")
                        .status(HttpStatus.OK.value())
                        .data(stats)
                        .build()
        );
    }
}
