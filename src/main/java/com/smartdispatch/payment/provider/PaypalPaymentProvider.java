package com.smartdispatch.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PayPal provider placeholder.
 * Best for: international payments, subscriptions, cross-border.
 */
@Component("paypalProvider")
@Slf4j
public class PaypalPaymentProvider implements PaymentProvider {

    @Value("${payment.paypal.client-id:mock_paypal_client_id}")
    private String clientId;

    @Value("${payment.paypal.client-secret:mock_paypal_client_secret}")
    private String clientSecret;

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "paypal", fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        String txnId = "PPL-" + System.currentTimeMillis();
        log.info("[PAYPAL] Payment of ${} initiated. TxnID: {}", amount, txnId);
        return new PaymentResult(true, txnId, null, null);
    }

    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        log.info("[PAYPAL] Refund of ${} for txn: {}", amount, transactionId);
        return new PaymentResult(true, "REF-" + transactionId, null, null);
    }

    @Override
    public String getProviderName() { return "PAYPAL"; }

    public PaymentResult fallbackPayment(Double amount, String customerId, String orderId, Throwable t) {
        log.error("[PAYPAL] Circuit breaker tripped or execution failed: {}", t.getMessage());
        throw new RuntimeException("PayPal unavailable", t); // Triggers orchestrator fallback
    }
}
