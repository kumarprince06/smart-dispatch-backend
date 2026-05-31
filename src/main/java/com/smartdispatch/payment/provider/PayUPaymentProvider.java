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

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "payu", fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        try {
            String txnid = "TXN-" + System.currentTimeMillis();
            String productInfo = "Smart Dispatch Order " + orderId;
            String firstName = "Customer"; // Usually fetch from DB
            String email = "customer@example.com"; 

            // PayU Hash sequence: key|txnid|amount|productinfo|firstname|email|||||||||||salt
            String hashString = key + "|" + txnid + "|" + amount + "|" + productInfo + "|" + firstName + "|" + email + "|||||||||||" + salt;
            String hash = hashCal("SHA-512", hashString);

            // Construct PayU redirect URL. For test environment: https://test.payu.in/_payment
            // We return a mock form redirect URL that frontend can open
            String payuUrl = "https://test.payu.in/_payment?key=" + key + "&txnid=" + txnid + "&amount=" + amount + "&productinfo=" + productInfo + "&firstname=" + firstName + "&email=" + email + "&hash=" + hash;
            
            log.info("[PAYU] Payment hash generated for Order {}", orderId);
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

    public PaymentResult fallbackPayment(Double amount, String customerId, String orderId, Throwable t) {
        throw new RuntimeException("PayU unavailable", t);
    }
}
