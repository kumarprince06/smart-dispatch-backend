package com.smartdispatch.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Cashfree provider placeholder.
 * Best for: India UPI, bank transfers, low MDR.
 */
@Component("cashfreeProvider")
@Slf4j
public class CashfreePaymentProvider implements PaymentProvider {

    @Override
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
}
