package com.smartdispatch.payment.dto;

import com.smartdispatch.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSessionRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Payment method/provider is required")
    private PaymentMethod provider;
}
