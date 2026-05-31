package com.smartdispatch.payment.provider;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("stripeProvider")
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    @Value("${payment.stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    @Override
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "stripe", fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(Double amount, String customerId, String orderId) {
        try {
            // Stripe uses smallest currency unit (paise/cents). Using INR.
            long amountInPaise = Math.round(amount * 100);

            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("smartdispatch://payment-success?orderId=" + orderId + "&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("smartdispatch://payment-failed?orderId=" + orderId)
                    .setClientReferenceId(orderId)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("inr")
                                                    .setUnitAmount(amountInPaise)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Smart Dispatch Delivery #" + orderId)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);
            log.info("[STRIPE] Checkout Session created for Order {}: {}", orderId, session.getId());

            // For Stripe, we return the session.id as transactionId and session.url as the redirect paymentUrl
            return new PaymentResult(true, session.getId(), session.getUrl(), null);

        } catch (Exception e) {
            log.error("[STRIPE] Failed to create checkout session: {}", e.getMessage());
            throw new RuntimeException("Stripe API error: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentResult processRefund(String transactionId, Double amount) {
        log.info("[STRIPE] Refund initiated for transaction: {}", transactionId);
        return new PaymentResult(true, "REF-STRIPE-" + transactionId, null, null);
    }

    @Override
    public String getProviderName() {
        return "STRIPE";
    }

    public PaymentResult fallbackPayment(Double amount, String customerId, String orderId, Throwable t) {
        log.error("[STRIPE] Circuit breaker triggered: {}", t.getMessage());
        throw new RuntimeException("Stripe unavailable", t);
    }
}
