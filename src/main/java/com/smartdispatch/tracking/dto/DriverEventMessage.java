package com.smartdispatch.tracking.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverEventMessage {

    private Long driverId;
    private String event;         // CONNECTED, DISCONNECTED, WENT_ONLINE, WENT_OFFLINE
    private String driverName;
    private LocalDateTime timestamp;
}
