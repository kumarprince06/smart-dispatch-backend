package com.smartdispatch.geofence.service;

import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.geofence.entity.ServiceZone;
import com.smartdispatch.geofence.repository.ServiceZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Geofencing service.
 * Validates that pickup and drop locations are within active service zones.
 * Rejects orders outside defined service areas.
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
     */
    public ServiceZone createZone(ServiceZone zone) {
        return zoneRepository.save(zone);
    }

    public List<ServiceZone> getAllZones() {
        return zoneRepository.findByActiveTrue();
    }

    public void deleteZone(Long id) {
        zoneRepository.deleteById(id);
    }
}
