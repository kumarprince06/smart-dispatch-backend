package com.smartdispatch.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Paystack payment provider placeholder.
 * Best for: African markets, card & mobile money payments.
 */
@Component("paystackProvider")
@Slf4j
public class PaystackPaymentProvider implements PaymentProvider {

    @Value("${payment.paystack.secret-key:mock_paystack_secret_key}")
    private String secretKey;

    @Value("${payment.paystack.public-key:mock_paystack_public_key}")
    private String publicKey;

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "paystack", fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        String txnId = "PSTK-" + System.currentTimeMillis();
        log.info("[PAYSTACK] Payment of ₹{} initiated. TxnID: {}", amount, txnId);
        // Paystack standard checkout: returns transaction ID and initialized state
        return new PaymentResult(true, txnId, publicKey, null);
    }

    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        log.info("[PAYSTACK] Refund of ₹{} for txn: {}", amount, transactionId);
        return new PaymentResult(true, "REF-" + transactionId, null, null);
    }

    @Override
    public String getProviderName() { return "PAYSTACK"; }

    public PaymentResult fallbackPayment(Double amount, String customerId, String orderId, Throwable t) {
        log.error("[PAYSTACK] Circuit breaker tripped or execution failed: {}", t.getMessage());
        throw new RuntimeException("Paystack unavailable", t); // Triggers orchestrator fallback
    }
}
