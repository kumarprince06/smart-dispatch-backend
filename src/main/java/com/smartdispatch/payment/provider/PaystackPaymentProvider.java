package com.smartdispatch.payment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Paystack payment provider — real integration via REST API.
 * Best for: African markets (Nigeria, Ghana, Kenya, South Africa).
 *
 * Flow:
 * 1. Backend calls POST /transaction/initialize → returns authorization_url + reference
 * 2. Frontend redirects user to authorization_url (Paystack Checkout)
 * 3. On payment complete, Paystack sends webhook → we verify + confirm
 * 4. Optionally, frontend calls GET /transaction/verify/:reference for instant verification
 *
 * Currency: Paystack uses the smallest unit (kobo for NGN, pesewas for GHS, cents for ZAR/KES).
 */
@Component("paystackProvider")
@Slf4j
public class PaystackPaymentProvider implements PaymentProvider {

    private static final String PAYSTACK_BASE_URL = "https://api.paystack.co";

    @Value("${payment.paystack.secret-key:mock_paystack_secret_key}")
    private String secretKey;

    @Value("${payment.paystack.public-key:mock_paystack_public_key}")
    private String publicKey;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        log.info("[PAYSTACK] Provider initialized. PublicKey: {}...", publicKey.substring(0, Math.min(12, publicKey.length())));
    }

    // ═══════════════════════════════════════════
    // Initialize Transaction (Create Payment Session)
    // ═══════════════════════════════════════════
    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "paystack", fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        try {
            // Paystack amounts are in the smallest currency unit (e.g., kobo = NGN * 100)
            long amountInSmallestUnit = Math.round(amount * 100);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", amountInSmallestUnit);
            requestBody.put("currency", "NGN"); // Default to NGN for African market
            requestBody.put("email", "customer_" + customerId + "@smartdispatch.com"); // Paystack requires email
            requestBody.put("reference", "SD-" + orderId + "-" + System.currentTimeMillis());
            requestBody.put("callback_url", "smartdispatch://payment-success?orderId=" + orderId);

            // Metadata for reconciliation
            Map<String, String> metadata = new HashMap<>();
            metadata.put("internal_order_id", orderId);
            metadata.put("customer_id", customerId);
            requestBody.put("metadata", metadata);

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    PAYSTACK_BASE_URL + "/transaction/initialize",
                    HttpMethod.POST, entity, String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.path("status").asBoolean()) {
                JsonNode data = root.path("data");
                String authorizationUrl = data.path("authorization_url").asText();
                String reference = data.path("reference").asText();
                String accessCode = data.path("access_code").asText();

                log.info("[PAYSTACK] Transaction initialized. Reference: {}, Order: {}, Amount: {} kobo",
                        reference, orderId, amountInSmallestUnit);

                // transactionId = Paystack reference | paymentUrl = authorization_url (for redirect)
                return new PaymentResult(true, reference, authorizationUrl, null);
            } else {
                String message = root.path("message").asText("Paystack initialization failed");
                log.error("[PAYSTACK] Initialization failed: {}", message);
                throw new RuntimeException("Paystack error: " + message);
            }

        } catch (Exception e) {
            log.error("[PAYSTACK] Failed to initialize transaction: {}", e.getMessage());
            throw new RuntimeException("Paystack API error: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════
    // Verify Transaction (after checkout completes)
    // ═══════════════════════════════════════════
    public VerificationResult verifyTransaction(String reference) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    PAYSTACK_BASE_URL + "/transaction/verify/" + reference,
                    HttpMethod.GET, entity, String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.path("status").asBoolean()) {
                JsonNode data = root.path("data");
                String status = data.path("status").asText();           // "success", "failed", "abandoned"
                String gatewayResponse = data.path("gateway_response").asText();
                long amountPaid = data.path("amount").asLong();
                String paystackId = String.valueOf(data.path("id").asLong());
                String channel = data.path("channel").asText();         // "card", "bank", "ussd", "mobile_money"

                log.info("[PAYSTACK] Verification: Reference={}, Status={}, Gateway={}, Channel={}",
                        reference, status, gatewayResponse, channel);

                return new VerificationResult(
                        "success".equals(status),
                        reference,
                        paystackId,
                        status,
                        gatewayResponse,
                        amountPaid,
                        channel
                );
            } else {
                log.error("[PAYSTACK] Verification failed for reference: {}", reference);
                return new VerificationResult(false, reference, null, "failed",
                        "Verification request failed", 0, null);
            }

        } catch (Exception e) {
            log.error("[PAYSTACK] Verification error for reference {}: {}", reference, e.getMessage());
            return new VerificationResult(false, reference, null, "error", e.getMessage(), 0, null);
        }
    }

    // ═══════════════════════════════════════════
    // Process Refund
    // ═══════════════════════════════════════════
    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        try {
            long amountInSmallestUnit = Math.round(amount * 100);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("transaction", transactionId); // Paystack reference or transaction ID
            requestBody.put("amount", amountInSmallestUnit);

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    PAYSTACK_BASE_URL + "/refund",
                    HttpMethod.POST, entity, String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.path("status").asBoolean()) {
                JsonNode data = root.path("data");
                String refundId = String.valueOf(data.path("id").asLong());
                log.info("[PAYSTACK] Refund created. RefundID: {}, Amount: {} kobo, TxnRef: {}",
                        refundId, amountInSmallestUnit, transactionId);
                return new PaymentResult(true, "REF-PSTK-" + refundId, null, null);
            } else {
                String message = root.path("message").asText("Refund failed");
                log.error("[PAYSTACK] Refund failed: {}", message);
                return new PaymentResult(false, null, null, "Paystack refund failed: " + message);
            }

        } catch (Exception e) {
            log.error("[PAYSTACK] Refund error for txn {}: {}", transactionId, e.getMessage());
            return new PaymentResult(false, null, null, "Paystack refund error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════
    // Webhook Signature Verification (HMAC SHA-512)
    // ═══════════════════════════════════════════
    public boolean verifyWebhookSignature(String payload, String paystackSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            boolean valid = hexString.toString().equals(paystackSignature);
            if (!valid) {
                log.warn("[PAYSTACK] Webhook signature mismatch!");
            }
            return valid;
        } catch (Exception e) {
            log.error("[PAYSTACK] Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() { return "PAYSTACK"; }

    public String getPublicKey() { return publicKey; }
    public String getSecretKey() { return secretKey; }

    // ═══════════════════════════════════════════
    // Fallback (Circuit Breaker)
    // ═══════════════════════════════════════════
    public PaymentResult fallbackPayment(Double amount, String customerId, String orderId, Throwable t) {
        log.error("[PAYSTACK] Circuit breaker tripped: {}", t.getMessage());
        throw new RuntimeException("Paystack unavailable", t);
    }

    // ═══════════════════════════════════════════
    // Helper: Create Authorization Headers
    // ═══════════════════════════════════════════
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + secretKey);
        return headers;
    }

    // ═══════════════════════════════════════════
    // Verification Result Record
    // ═══════════════════════════════════════════
    public record VerificationResult(
            boolean success,
            String reference,
            String paystackTransactionId,
            String status,
            String gatewayResponse,
            long amountInKobo,
            String channel
    ) {}
}
