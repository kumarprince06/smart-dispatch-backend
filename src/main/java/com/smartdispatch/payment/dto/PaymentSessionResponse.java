package com.smartdispatch.payment.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSessionResponse {

    private Long paymentId;        // Our internal transaction ID
    private String transactionId;  // Our internal TXN-XXXX
    private String paymentSessionId; // Razorpay order_id (e.g., order_xxxxxx)
    private String key;            // Razorpay public key (safe to send to client)
    private Long orderId;
    private Double amount;
    private String currency;
    private String provider;
}
