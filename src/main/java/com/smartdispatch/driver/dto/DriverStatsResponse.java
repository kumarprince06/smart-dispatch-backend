package com.smartdispatch.driver.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatsResponse {

    private Long totalDrivers;
    private Long availableDrivers;
    private Long onlineDrivers;
    private Long busyDrivers;
    private Long onDeliveryDrivers;
    private Long offlineDrivers;
    private Long onBreakDrivers;
    private Long suspendedDrivers;
    private Long pendingVerification;
    private Long verifiedDrivers;
    private Long rejectedDrivers;
}
