package com.smartdispatch.geofence.repository;

import com.smartdispatch.geofence.entity.ServiceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceZoneRepository extends JpaRepository<ServiceZone, Long> {

    List<ServiceZone> findByActiveTrue();

    Optional<ServiceZone> findByName(String name);

    @Query("SELECT z FROM ServiceZone z WHERE z.active = true AND " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(z.centerLatitude)) * " +
            "cos(radians(z.centerLongitude) - radians(:lng)) + " +
            "sin(radians(:lat)) * sin(radians(z.centerLatitude)))) <= z.radiusKm")
    List<ServiceZone> findZonesContainingPoint(@Param("lat") Double lat, @Param("lng") Double lng);
}
