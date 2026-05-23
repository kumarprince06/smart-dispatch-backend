package com.smartdispatch.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PayPal provider placeholder.
 * Best for: international payments, subscriptions, cross-border.
 */
@Component("paypalProvider")
@Slf4j
public class PaypalPaymentProvider implements PaymentProvider {

    @Override
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        String txnId = "PPL-" + System.currentTimeMillis();
        log.info("[PAYPAL] Payment of ${} initiated. TxnID: {}", amount, txnId);
        return new PaymentResult(true, txnId, null);
    }

    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        log.info("[PAYPAL] Refund of ${} for txn: {}", amount, transactionId);
        return new PaymentResult(true, "REF-" + transactionId, null);
    }

    @Override
    public String getProviderName() { return "PAYPAL"; }
}
