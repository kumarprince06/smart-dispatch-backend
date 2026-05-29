package com.smartdispatch.dispatch.service;

import com.smartdispatch.dispatch.dto.DispatchResult;
import com.smartdispatch.dispatch.enums.DispatchStrategy;
import com.smartdispatch.driver.entity.Driver;
import com.smartdispatch.driver.enums.DriverStatus;
import com.smartdispatch.driver.enums.VerificationStatus;
import com.smartdispatch.driver.repository.DriverRepository;
import com.smartdispatch.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Intelligent Dispatch Engine.
 * Uses Redis GEO for ultra-fast nearest driver search
 * with expanding radius, strategy pattern, and heartbeat validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private final GeoLocationService geoLocationService;
    private final DriverRepository driverRepository;

    // Expanding radius stages (km)
    private static final double[] RADIUS_STAGES = {3.0, 5.0, 10.0, 15.0, 25.0};

    // ═══════════════════════════════════════════
    // Find & Assign Nearest Driver
    // ═══════════════════════════════════════════
    public DispatchResult findNearestDriver(
            Double pickupLat,
            Double pickupLng,
            DispatchStrategy strategy
    ) {
        long startTime = System.currentTimeMillis();
        int totalSearched = 0;

        // Expanding radius search
        for (double radius : RADIUS_STAGES) {

            GeoResults<RedisGeoCommands.GeoLocation<Object>> nearbyDrivers =
                    geoLocationService.getNearbyDrivers(pickupLat, pickupLng, radius);

            if (nearbyDrivers == null || nearbyDrivers.getContent().isEmpty()) {
                log.debug("No drivers found within {}km, expanding radius...", radius);
                continue;
            }

            // Filter to only eligible drivers
            List<DriverCandidate> candidates = new ArrayList<>();

            for (var geoResult : nearbyDrivers.getContent()) {
                totalSearched++;

                String driverIdStr = geoResult.getContent().getName().toString();
                Long driverId = Long.valueOf(driverIdStr);

                // Check heartbeat (stale detection)
                if (!geoLocationService.isDriverActive(driverId)) {
                    log.debug("Driver {} heartbeat expired, skipping", driverId);
                    continue;
                }

                // Check DB status
                Driver driver = driverRepository.findById(driverId).orElse(null);
                if (driver == null) continue;
                if (driver.getStatus() != DriverStatus.AVAILABLE) continue;
                if (driver.getVerificationStatus() != VerificationStatus.VERIFIED) continue;
                if (!driver.getActive()) continue;
                if (!driver.canAcceptOrder()) continue;

                double distanceKm = geoResult.getDistance() != null
                        ? geoResult.getDistance().getValue()
                        : 0.0;

                candidates.add(new DriverCandidate(driver, distanceKm));
            }

            if (candidates.isEmpty()) {
                log.debug("No eligible drivers within {}km, expanding...", radius);
                continue;
            }

            // Apply dispatch strategy
            Driver selectedDriver = applyStrategy(candidates, strategy);
            double selectedDistance = candidates.stream()
                    .filter(c -> c.driver.getId().equals(selectedDriver.getId()))
                    .findFirst()
                    .map(c -> c.distanceKm)
                    .orElse(0.0);

            long dispatchTime = System.currentTimeMillis() - startTime;

            log.info("Dispatch: Driver {} selected in {}ms ({}km away, radius: {}km, searched: {})",
                    selectedDriver.getId(), dispatchTime, selectedDistance, radius, totalSearched);

            return DispatchResult.builder()
                    .driverId(selectedDriver.getId())
                    .driverName(selectedDriver.getUser().getFirstName() + " " + selectedDriver.getUser().getLastName())
                    .vehicleNumber(selectedDriver.getVehicleNumber())
                    .distanceKm(Math.round(selectedDistance * 100.0) / 100.0)
                    .estimatedMinutes((int) Math.ceil(selectedDistance * 3)) // ~3 min per km
                    .searchRadiusKm(radius)
                    .driversSearched(totalSearched)
                    .dispatchTimeMs(dispatchTime)
                    .build();
        }

        throw new BadRequestException("No drivers available within " + RADIUS_STAGES[RADIUS_STAGES.length - 1] + "km radius");
    }

    // ═══════════════════════════════════════════
    // Strategy Pattern Implementation
    // ═══════════════════════════════════════════
    private Driver applyStrategy(List<DriverCandidate> candidates, DispatchStrategy strategy) {
        return switch (strategy) {
            case NEAREST -> candidates.stream()
                    .min(Comparator.comparingDouble(c -> c.distanceKm))
                    .map(c -> c.driver)
                    .orElseThrow();

            case LEAST_BUSY -> candidates.stream()
                    .min(Comparator.comparingInt(c -> c.driver.getActiveOrderCount()))
                    .map(c -> c.driver)
                    .orElseThrow();

            case HIGHEST_RATED -> candidates.stream()
                    .max(Comparator.comparingDouble(c -> c.driver.getRating()))
                    .map(c -> c.driver)
                    .orElseThrow();

            case ROUND_ROBIN -> candidates.stream()
                    .min(Comparator.comparingInt(c -> c.driver.getTotalTrips()))
                    .map(c -> c.driver)
                    .orElseThrow();
        };
    }

    // Internal record for candidate tracking
    private record DriverCandidate(Driver driver, double distanceKm) {}
}
