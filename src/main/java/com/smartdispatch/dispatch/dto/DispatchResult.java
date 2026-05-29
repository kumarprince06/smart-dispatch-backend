package com.smartdispatch.dispatch.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchResult {

    private Long driverId;
    private String driverName;
    private String vehicleNumber;
    private Double distanceKm;
    private Integer estimatedMinutes;
    private Double searchRadiusKm;
    private Integer driversSearched;
    private Long dispatchTimeMs;
}
