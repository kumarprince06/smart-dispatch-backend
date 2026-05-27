package com.smartdispatch.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignDriverRequest {
    @NotNull(message = "Driver ID is required")
    private Long driverId;
}
