package com.smartdispatch.order.dto;

import com.smartdispatch.order.enums.OrderPriority;
import com.smartdispatch.order.enums.PackageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    // Pickup
    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotNull(message = "Pickup latitude is required")
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    private Double pickupLongitude;

    private String pickupContactName;
    private String pickupContactPhone;

    // Drop
    @NotBlank(message = "Drop address is required")
    private String dropAddress;

    @NotNull(message = "Drop latitude is required")
    private Double dropLatitude;

    @NotNull(message = "Drop longitude is required")
    private Double dropLongitude;

    private String dropContactName;
    private String dropContactPhone;

    // Package
    private PackageType packageType;
    private String packageDescription;
    private Double packageWeightKg;

    // Priority
    private OrderPriority priority;

    // Notes
    private String customerNotes;
}
