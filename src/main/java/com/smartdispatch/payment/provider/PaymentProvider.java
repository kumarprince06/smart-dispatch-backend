package com.smartdispatch.payment.provider;

/**
 * Strategy interface for payment providers.
 * Add new payment gateway? Just implement this interface.
 */
public interface PaymentProvider {

    PaymentResult processPayment(Double amount, String customerId, String orderId);

    PaymentResult processRefund(String transactionId, Double amount);

    String getProviderName();

    record PaymentResult(boolean success, String transactionId, String failureReason) {}
}
