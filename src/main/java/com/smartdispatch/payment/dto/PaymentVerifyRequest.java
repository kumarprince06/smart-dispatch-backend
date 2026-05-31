package com.smartdispatch.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerifyRequest {

    @NotBlank(message = "razorpay_payment_id is required")
    private String razorpayPaymentId;

    @NotBlank(message = "razorpay_order_id is required")
    private String razorpayOrderId;

    @NotBlank(message = "razorpay_signature is required")
    private String razorpaySignature;
}
