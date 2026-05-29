package com.smartdispatch.geofence.service;

import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.geofence.entity.ServiceZone;
import com.smartdispatch.geofence.repository.ServiceZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Geofencing service.
 * Validates that pickup and drop locations are within active service zones.
 * Rejects orders outside defined service areas.
 *
 * CACHING STRATEGY:
 * - getAllZones() is cached for 30 minutes (zones almost never change)
 * - createZone() and deleteZone() evict the cache so fresh data is loaded
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceService {

    private final ServiceZoneRepository zoneRepository;

    /**
     * Validate that a point is within at least one active service zone.
     */
    public ServiceZone validateLocation(Double lat, Double lng) {
        List<ServiceZone> zones = zoneRepository.findZonesContainingPoint(lat, lng);

        if (zones.isEmpty()) {
            throw new BadRequestException(
                    "Location (" + lat + ", " + lng + ") is outside all service zones. " +
                            "We don't serve this area yet."
            );
        }

        ServiceZone zone = zones.get(0);
        log.debug("Location ({}, {}) is within zone: {}", lat, lng, zone.getName());
        return zone;
    }

    /**
     * Validate both pickup and drop locations.
     */
    public void validateOrderLocations(Double pickupLat, Double pickupLng,
                                        Double dropLat, Double dropLng) {
        validateLocation(pickupLat, pickupLng);
        validateLocation(dropLat, dropLng);
    }

    /**
     * Get zone-specific surge multiplier.
     */
    public Double getZoneMultiplier(Double lat, Double lng) {
        List<ServiceZone> zones = zoneRepository.findZonesContainingPoint(lat, lng);
        if (zones.isEmpty()) return 1.0;
        return zones.get(0).getZoneMultiplier();
    }

    /**
     * CRUD for zones (Admin).
     * getAllZones is cached because zone data rarely changes.
     */
    @CacheEvict(value = "service-zones", allEntries = true)
    public ServiceZone createZone(ServiceZone zone) {
        log.info("Creating new service zone: {}. Cache evicted.", zone.getName());
        return zoneRepository.save(zone);
    }

    @Cacheable(value = "service-zones", key = "'all-active'")
    public List<ServiceZone> getAllZones() {
        log.info("Cache MISS: Loading all active service zones from database");
        return zoneRepository.findByActiveTrue();
    }

    @CacheEvict(value = "service-zones", allEntries = true)
    public void deleteZone(Long id) {
        log.info("Deleting service zone: {}. Cache evicted.", id);
        zoneRepository.deleteById(id);
    }
}
