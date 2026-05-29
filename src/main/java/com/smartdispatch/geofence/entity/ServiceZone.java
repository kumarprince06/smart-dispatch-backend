package com.smartdispatch.geofence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_zones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // e.g., "Kolkata-Central", "Delhi-NCR"

    private String city;

    // Zone center point
    private Double centerLatitude;

    private Double centerLongitude;

    // Service radius (km)
    private Double radiusKm;

    @Builder.Default
    private Boolean active = true;

    // Surge multiplier for this zone
    @Builder.Default
    private Double zoneMultiplier = 1.0;
}
