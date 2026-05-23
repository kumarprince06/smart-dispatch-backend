package com.smartdispatch.tracking.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateMessage {

    private Long driverId;
    private Long orderId;
    private Double latitude;
    private Double longitude;
    private Double speed;          // km/h (from driver app GPS)
    private Double heading;        // direction in degrees (0-360)
    private Double estimatedMinutes; // ETA to destination
    private LocalDateTime timestamp;
}
