package com.smartdispatch.dispatch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis GEO-based location service.
 * Stores driver locations in Redis for ultra-fast geo-spatial queries.
 * O(log N) complexity for nearby search vs O(N) for SQL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeoLocationService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String DRIVER_GEO_KEY = "drivers:geo";
    private static final String DRIVER_HEARTBEAT_KEY = "drivers:heartbeat:";
    private static final long HEARTBEAT_TTL_SECONDS = 60; // Driver considered stale after 60s

    // ═══════════════════════════════════════════
    // Update Driver Location in Redis GEO
    // ═══════════════════════════════════════════
    public void updateDriverLocation(Long driverId, Double latitude, Double longitude) {
        // Redis GEO uses (longitude, latitude) format
        Point point = new Point(longitude, latitude);
        redisTemplate.opsForGeo().add(DRIVER_GEO_KEY, point, driverId.toString());

        // Update heartbeat (TTL-based stale detection)
        redisTemplate.opsForValue().set(
                DRIVER_HEARTBEAT_KEY + driverId,
                System.currentTimeMillis(),
                HEARTBEAT_TTL_SECONDS,
                TimeUnit.SECONDS
        );

        log.debug("Driver {} location updated: ({}, {})", driverId, latitude, longitude);
    }

    // ═══════════════════════════════════════════
    // Find Nearby Drivers (Redis GEO Radius)
    // ═══════════════════════════════════════════
    public GeoResults<RedisGeoCommands.GeoLocation<Object>> getNearbyDrivers(
            Double latitude, Double longitude, Double radiusKm
    ) {
        Circle area = new Circle(
                new Point(longitude, latitude),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        // Include distance in results and sort by distance ASC
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(50); // Max 50 drivers

        return redisTemplate.opsForGeo().radius(DRIVER_GEO_KEY, area, args);
    }

    // ═══════════════════════════════════════════
    // Get Distance Between Two Points
    // ═══════════════════════════════════════════
    public Distance getDistanceBetween(Long driverId1, Long driverId2) {
        return redisTemplate.opsForGeo().distance(
                DRIVER_GEO_KEY,
                driverId1.toString(),
                driverId2.toString(),
                Metrics.KILOMETERS
        );
    }

    // ═══════════════════════════════════════════
    // Remove Driver from GEO (when offline)
    // ═══════════════════════════════════════════
    public void removeDriver(Long driverId) {
        redisTemplate.opsForGeo().remove(DRIVER_GEO_KEY, driverId.toString());
        redisTemplate.delete(DRIVER_HEARTBEAT_KEY + driverId);
        log.info("Driver {} removed from geo index", driverId);
    }

    // ═══════════════════════════════════════════
    // Check if Driver Heartbeat is Active
    // ═══════════════════════════════════════════
    public boolean isDriverActive(Long driverId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(DRIVER_HEARTBEAT_KEY + driverId));
    }

    // ═══════════════════════════════════════════
    // Get Driver Position
    // ═══════════════════════════════════════════
    public List<Point> getDriverPosition(Long driverId) {
        return redisTemplate.opsForGeo().position(DRIVER_GEO_KEY, driverId.toString());
    }
}
