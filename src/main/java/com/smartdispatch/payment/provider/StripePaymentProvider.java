package com.smartdispatch.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stripe payment provider placeholder.
 * Replace with real Stripe SDK integration.
 */
@Component("stripeProvider")
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    @Override
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        // TODO: Integrate Stripe SDK — create PaymentIntent
        String txnId = "STR-" + System.currentTimeMillis();
        log.info("[STRIPE] Payment of ₹{} initiated. TxnID: {}", amount, txnId);
        return new PaymentResult(true, txnId, null);
    }

    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        log.info("[STRIPE] Refund of ₹{} for txn: {}", amount, transactionId);
        return new PaymentResult(true, "REF-" + transactionId, null);
    }

    @Override
    public String getProviderName() { return "STRIPE"; }
}
