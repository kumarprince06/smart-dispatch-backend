package com.smartdispatch.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stripe payment provider placeholder.
 * Replace with real Stripe SDK integration.
 */
@Component("stripeProvider")
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    @Value("${payment.stripe.public-key:mock_pk_stripe}")
    private String publicKey;

    @Value("${payment.stripe.secret-key:mock_sk_stripe}")
    private String secretKey;

    @Value("${payment.stripe.webhook-secret:mock_wh_stripe}")
    private String webhookSecret;

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "stripe", fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        // TODO: Integrate Stripe SDK — create PaymentIntent
        String txnId = "STR-" + System.currentTimeMillis();
        log.info("[STRIPE] Payment of ₹{} initiated. TxnID: {}", amount, txnId);
        return new PaymentResult(true, txnId, null, null);
    }

    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        log.info("[STRIPE] Refund of ₹{} for txn: {}", amount, transactionId);
        return new PaymentResult(true, "REF-" + transactionId, null, null);
    }

    @Override
    public String getProviderName() { return "STRIPE"; }

    public PaymentResult fallbackPayment(Double amount, String customerId, String orderId, Throwable t) {
        log.error("[STRIPE] Circuit breaker tripped or execution failed: {}", t.getMessage());
        throw new RuntimeException("Stripe unavailable", t); // Triggers orchestrator fallback
    }
}
