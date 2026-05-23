package com.smartdispatch.driver.dto;

import com.smartdispatch.driver.enums.DriverStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDriverStatusRequest {

    @NotNull(message = "Status is required")
    private DriverStatus status;
}
