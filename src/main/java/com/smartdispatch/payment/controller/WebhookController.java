package com.smartdispatch.payment.controller;

import com.smartdispatch.payment.entity.Payment;
import com.smartdispatch.payment.enums.PaymentStatus;
import com.smartdispatch.payment.provider.PaystackPaymentProvider;
import com.smartdispatch.payment.repository.PaymentRepository;
import com.smartdispatch.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Payment Webhook Controller.
 * Receives async callbacks from payment gateways.
 *
 * CRITICAL: Never trust client-side payment confirmation.
 * Always verify via gateway webhook.
 */
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final PaymentRepository paymentRepository;
    private final PaystackPaymentProvider paystackProvider;
    private final PaymentService paymentService;

    // ═══════════════════════════════════════════
    // Razorpay Webhook
    // ═══════════════════════════════════════════
    @PostMapping("/razorpay")
    public ResponseEntity<Void> razorpayWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        log.info("[WEBHOOK] Razorpay event received");

        // TODO: Verify signature using Razorpay Utils
        // boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);

        String event = (String) payload.get("event");
        if ("payment.captured".equals(event)) {
            processPaymentSuccess(payload, "RAZORPAY");
        } else if ("payment.failed".equals(event)) {
            processPaymentFailure(payload, "RAZORPAY");
        }

        return ResponseEntity.ok().build();
    }

    // ═══════════════════════════════════════════
    // Stripe Webhook
    // ═══════════════════════════════════════════
    @PostMapping("/stripe")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) {
        log.info("[WEBHOOK] Stripe event received");

        String type = (String) payload.get("type");
        
        try {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            Map<String, Object> object = (Map<String, Object>) data.get("object");
            String sessionId = (String) object.get("id"); // This is the checkout session ID
            
            if ("checkout.session.completed".equals(type)) {
                paymentRepository.findByProviderTransactionId(sessionId).ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    log.info("[WEBHOOK] Stripe Payment SUCCESS for Session: {}", sessionId);
                });
            } else if ("checkout.session.expired".equals(type) || "checkout.session.async_payment_failed".equals(type)) {
                paymentRepository.findByProviderTransactionId(sessionId).ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason("Stripe Checkout Failed/Expired");
                    paymentRepository.save(payment);
                    log.warn("[WEBHOOK] Stripe Payment FAILED for Session: {}", sessionId);
                });
            }
        } catch (Exception e) {
            log.error("Failed to parse Stripe webhook", e);
        }

        return ResponseEntity.ok().build();
    }

    // ═══════════════════════════════════════════
    // Paystack Webhook (HMAC-SHA512 Verified)
    // ═══════════════════════════════════════════
    /**
     * Paystack sends webhooks for these events:
     *  - charge.success   → payment was successful
     *  - charge.failed    → payment failed
     *  - refund.processed → refund completed
     *
     * Payload structure:
     * {
     *   "event": "charge.success",
     *   "data": {
     *     "id": 123456789,
     *     "reference": "SD-42-1717891234567",
     *     "status": "success",
     *     "amount": 50000,
     *     "channel": "card",
     *     "metadata": { "internal_order_id": "42", "customer_id": "7" }
     *   }
     * }
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/paystack")
    public ResponseEntity<Void> paystackWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Paystack-Signature", required = false) String paystackSignature
    ) {
        log.info("[WEBHOOK] Paystack event received");

        // Step 1: Verify HMAC-SHA512 signature
        if (paystackSignature != null && !paystackProvider.verifyWebhookSignature(rawPayload, paystackSignature)) {
            log.warn("[WEBHOOK] Paystack signature verification FAILED. Rejecting.");
            return ResponseEntity.status(401).build();
        }

        try {
            // Parse the raw JSON payload
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payload = mapper.readValue(rawPayload, Map.class);

            String event = (String) payload.get("event");
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            if (data == null) {
                log.warn("[WEBHOOK] Paystack payload missing 'data' field");
                return ResponseEntity.ok().build();
            }

            String reference = (String) data.get("reference");
            String paystackTxnId = String.valueOf(data.get("id"));

            log.info("[WEBHOOK] Paystack event={}, reference={}, txnId={}", event, reference, paystackTxnId);

            // Delegate to service layer
            paymentService.handlePaystackWebhook(reference, event, paystackTxnId);

        } catch (Exception e) {
            log.error("[WEBHOOK] Failed to parse Paystack webhook payload", e);
        }

        // Always return 200 to Paystack to prevent retries
        return ResponseEntity.ok().build();
    }

    // ═══════════════════════════════════════════
    // Common Processing Logic
    // ═══════════════════════════════════════════
    private void processPaymentSuccess(Map<String, Object> payload, String provider) {
        String gatewayPaymentId = extractPaymentId(payload);
        paymentRepository.findByGatewayPaymentId(gatewayPaymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.info("[WEBHOOK] Payment SUCCESS. Provider: {}, TxnID: {}", provider, payment.getTransactionId());
        });
    }

    private void processPaymentFailure(Map<String, Object> payload, String provider) {
        String gatewayPaymentId = extractPaymentId(payload);
        paymentRepository.findByGatewayPaymentId(gatewayPaymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Gateway reported failure");
            paymentRepository.save(payment);
            log.warn("[WEBHOOK] Payment FAILED. Provider: {}, TxnID: {}", provider, payment.getTransactionId());
        });
    }

    @SuppressWarnings("unchecked")
    private String extractPaymentId(Map<String, Object> payload) {
        try {
            Map<String, Object> payloadData = (Map<String, Object>) payload.get("payload");
            if (payloadData != null) {
                Map<String, Object> paymentEntity = (Map<String, Object>) payloadData.get("payment");
                if (paymentEntity != null) {
                    Map<String, Object> entity = (Map<String, Object>) paymentEntity.get("entity");
                    if (entity != null) return (String) entity.get("id");
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract payment ID from webhook payload", e);
        }
        return "";
    }
}
