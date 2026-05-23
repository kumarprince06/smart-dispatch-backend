package com.smartdispatch.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Razorpay payment provider placeholder.
 * Replace with real Razorpay SDK integration.
 */
@Component("razorpayProvider")
@Slf4j
public class RazorpayPaymentProvider implements PaymentProvider {

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "razorpay", fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        // TODO: Integrate Razorpay SDK — create order, return payment link
        String txnId = "RZP-" + System.currentTimeMillis();
        log.info("[RAZORPAY] Payment of ₹{} initiated. TxnID: {}", amount, txnId);
        return new PaymentResult(true, txnId, null);
    }

    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        log.info("[RAZORPAY] Refund of ₹{} for txn: {}", amount, transactionId);
        return new PaymentResult(true, "REF-" + transactionId, null);
    }

    @Override
    public String getProviderName() { return "RAZORPAY"; }

    public PaymentResult fallbackPayment(Double amount, String customerId, String orderId, Throwable t) {
        log.error("[RAZORPAY] Circuit breaker tripped or execution failed: {}", t.getMessage());
        throw new RuntimeException("Razorpay unavailable", t); // Triggers orchestrator fallback
    }
}
