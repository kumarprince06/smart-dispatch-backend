package com.smartdispatch.payment.dto;

import com.smartdispatch.payment.enums.PaymentMethod;
import com.smartdispatch.payment.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private String transactionId;
    private Long orderId;
    private Double amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String providerTransactionId;
    private String failureReason;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
