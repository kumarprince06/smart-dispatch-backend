package com.smartdispatch.tracking.service;

import com.smartdispatch.tracking.dto.DriverEventMessage;
import com.smartdispatch.tracking.dto.LocationUpdateMessage;
import com.smartdispatch.tracking.dto.OrderStatusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time event broadcasting service.
 * Pushes live updates to connected WebSocket clients.
 *
 * Topics:
 *   /topic/order/{orderId}         → Live driver location for specific order
 *   /topic/order-status/{orderId}  → Order status changes
 *   /topic/driver-events           → Admin: driver online/offline events
 *   /topic/admin/live-stats        → Admin: live dashboard stats
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final SimpMessagingTemplate messagingTemplate;

    // Track last location update time per driver (throttle flooding)
    private final Map<Long, Long> lastUpdateTime = new ConcurrentHashMap<>();

    private static final long THROTTLE_MS = 3000; // Min 3 seconds between updates

    // ═══════════════════════════════════════════
    // Broadcast Live Driver Location
    // ═══════════════════════════════════════════
    public void broadcastDriverLocation(LocationUpdateMessage message) {
        // Throttle: prevent flooding (max 1 update per 3 sec per driver)
        Long lastUpdate = lastUpdateTime.get(message.getDriverId());
        long now = System.currentTimeMillis();

        if (lastUpdate != null && (now - lastUpdate) < THROTTLE_MS) {
            return; // Skip this update (too frequent)
        }

        lastUpdateTime.put(message.getDriverId(), now);
        message.setTimestamp(LocalDateTime.now());

        // Push to customer watching this order
        messagingTemplate.convertAndSend(
                "/topic/order/" + message.getOrderId(),
                message
        );

        log.debug("Location broadcast → order/{}: driver {} at ({}, {})",
                message.getOrderId(), message.getDriverId(),
                message.getLatitude(), message.getLongitude());
    }

    // ═══════════════════════════════════════════
    // Broadcast Order Status Change
    // ═══════════════════════════════════════════
    public void broadcastOrderStatus(OrderStatusMessage message) {
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/order-status/" + message.getOrderId(),
                message
        );

        log.info("Status broadcast → order-status/{}: {}",
                message.getOrderId(), message.getStatus());
    }

    // ═══════════════════════════════════════════
    // Broadcast Driver Event (Admin Dashboard)
    // ═══════════════════════════════════════════
    public void broadcastDriverEvent(DriverEventMessage message) {
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend(
                "/topic/driver-events",
                message
        );

        log.info("Driver event broadcast: {} → {}",
                message.getDriverName(), message.getEvent());
    }

    // ═══════════════════════════════════════════
    // Broadcast Live Admin Stats
    // ═══════════════════════════════════════════
    public void broadcastAdminStats(Object stats) {
        messagingTemplate.convertAndSend(
                "/topic/admin/live-stats",
                stats
        );
    }
}
