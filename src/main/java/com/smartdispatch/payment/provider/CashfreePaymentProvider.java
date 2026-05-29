package com.smartdispatch.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cashfree provider placeholder.
 * Best for: India UPI, bank transfers, low MDR.
 */
@Component("cashfreeProvider")
@Slf4j
public class CashfreePaymentProvider implements PaymentProvider {

    @Value("${payment.cashfree.app-id:mock_cashfree_app_id}")
    private String appId;

    @Value("${payment.cashfree.secret-key:mock_cashfree_secret_key}")
    private String secretKey;

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "cashfree", fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        String txnId = "CF-" + System.currentTimeMillis();
        log.info("[CASHFREE] Payment of ₹{} initiated. TxnID: {}", amount, txnId);
        return new PaymentResult(true, txnId, null);
    }

    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        log.info("[CASHFREE] Refund of ₹{} for txn: {}", amount, transactionId);
        return new PaymentResult(true, "REF-" + transactionId, null);
    }

    @Override
    public String getProviderName() { return "CASHFREE"; }

    public PaymentResult fallbackPayment(Double amount, String customerId, String orderId, Throwable t) {
        log.error("[CASHFREE] Circuit breaker tripped or execution failed: {}", t.getMessage());
        throw new RuntimeException("Cashfree unavailable", t); // Triggers orchestrator fallback
    }
}
