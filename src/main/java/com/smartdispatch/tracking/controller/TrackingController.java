package com.smartdispatch.tracking.controller;

import com.smartdispatch.dispatch.service.GeoLocationService;
import com.smartdispatch.tracking.dto.LocationUpdateMessage;
import com.smartdispatch.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for real-time tracking.
 *
 * Driver app sends location to: /app/location/update
 * Customer subscribes to:       /topic/order/{orderId}
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class TrackingController {

    private final TrackingService trackingService;
    private final GeoLocationService geoLocationService;

    // ═══════════════════════════════════════════
    // Driver sends live location
    // ═══════════════════════════════════════════
    @MessageMapping("/location/update")
    public void updateDriverLocation(@Payload LocationUpdateMessage message) {

        // 1. Update Redis GEO (for dispatch engine)
        geoLocationService.updateDriverLocation(
                message.getDriverId(),
                message.getLatitude(),
                message.getLongitude()
        );

        // 2. Broadcast to customer (live tracking)
        trackingService.broadcastDriverLocation(message);
    }
}
