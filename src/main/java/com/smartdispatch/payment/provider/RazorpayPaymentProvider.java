package com.smartdispatch.payment.provider;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Razorpay payment provider.
 * Creates a Razorpay Order. Frontend opens Razorpay Checkout SDK with this order_id.
 * After payment, frontend calls /payments/verify for signature validation.
 * Webhook is the source of truth for final status update.
 */
@Component("razorpayProvider")
@Slf4j
public class RazorpayPaymentProvider implements PaymentProvider {

    @Value("${payment.razorpay.key-id}")
    private String keyId;

    @Value("${payment.razorpay.key-secret}")
    private String keySecret;

    @Value("${payment.razorpay.webhook-secret}")
    private String webhookSecret;

    /**
     * Creates a Razorpay Order and returns (orderId, publicKey) for the frontend.
     * The transactionId = Razorpay order_id (e.g., "order_xxxxx")
     * The paymentUrl = the Razorpay public key (so frontend can open checkout)
     */
    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "razorpay", fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject req = new JSONObject();
            req.put("amount", (int) Math.round(amount * 100)); // paise
            req.put("currency", "INR");
            req.put("receipt", "rcpt_" + orderId);
            req.put("notes", new JSONObject().put("internal_order_id", orderId));

            Order rzpOrder = client.orders.create(req);
            String rzpOrderId = rzpOrder.get("id");

            log.info("[RAZORPAY] Order created. OrderID: {}, Internal: {}, Amount: ₹{}", rzpOrderId, orderId, amount);
            // transactionId = razorpay order_id | paymentUrl = public key (needed by frontend)
            return new PaymentResult(true, rzpOrderId, keyId, null);

        } catch (Exception e) {
            log.error("[RAZORPAY] Failed to create order: {}", e.getMessage());
            throw new RuntimeException("Razorpay API error: " + e.getMessage(), e);
        }
    }

    /**
     * Verify Razorpay signature after frontend checkout completes.
     * Returns true if valid.
     */
    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            return Utils.verifyPaymentSignature(
                    new JSONObject(Map.of(
                            "razorpay_order_id", razorpayOrderId,
                            "razorpay_payment_id", razorpayPaymentId,
                            "razorpay_signature", razorpaySignature
                    )),
                    keySecret
            );
        } catch (Exception e) {
            log.error("[RAZORPAY] Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        log.info("[RAZORPAY] Refund of ₹{} for txn: {}", amount, transactionId);
        return new PaymentResult(true, "REF-" + transactionId, null, null);
    }

    @Override
    public String getProviderName() { return "RAZORPAY"; }

    public String getKeyId() { return keyId; }
    public String getWebhookSecret() { return webhookSecret; }

    public PaymentResult fallbackPayment(Double amount, String customerId, String orderId, Throwable t) {
        log.error("[RAZORPAY] Circuit breaker triggered: {}", t.getMessage());
        throw new RuntimeException("Razorpay unavailable", t);
    }
}
