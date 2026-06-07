package com.smartdispatch.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component("payuProvider")
@Slf4j
public class PayUPaymentProvider implements PaymentProvider {

    @Value("${payment.payu.key}")
    private String key;

    @Value("${payment.payu.salt}")
    private String salt;

    @Value("${app.backend-url:http://192.168.1.10:8080}")
    private String backendUrl;

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "payu", fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        try {
            String txnid = "PAYU-" + System.currentTimeMillis();
            
            // Generate checkout url hosted by our backend to auto-POST to PayU
            String payuUrl = backendUrl + "/api/v1/payments/payu/checkout?txnid=" + txnid;
            
            log.info("[PAYU] Payment session generated for Order {}. Redirect URL: {}", orderId, payuUrl);
            return new PaymentResult(true, txnid, payuUrl, null);

        } catch (Exception e) {
            log.error("[PAYU] Failed to create payment request: {}", e.getMessage());
            throw new RuntimeException("PayU API error: " + e.getMessage(), e);
        }
    }

    public String hashCal(String type, String str) {
        byte[] hashseq = str.getBytes();
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest algorithm = MessageDigest.getInstance(type);
            algorithm.reset();
            algorithm.update(hashseq);
            byte messageDigest[] = algorithm.digest();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) hexString.append("0");
                hexString.append(hex);
            }
        } catch (NoSuchAlgorithmException nsae) { }
        return hexString.toString();
    }

    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        return new PaymentResult(true, "REF-PAYU-" + transactionId, null, null);
    }

    @Override
    public String getProviderName() {
        return "PAYU";
    }

    public String getKey() { return key; }
    public String getSalt() { return salt; }
    public String getBackendUrl() { return backendUrl; }

    public PaymentResult fallbackPayment(Double amount, String customerId, String orderId, Throwable t) {
        throw new RuntimeException("PayU unavailable", t);
    }
}
