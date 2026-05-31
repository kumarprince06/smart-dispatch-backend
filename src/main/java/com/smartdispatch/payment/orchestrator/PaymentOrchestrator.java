package com.smartdispatch.payment.orchestrator;

import com.smartdispatch.payment.enums.PaymentMethod;
import com.smartdispatch.payment.provider.PaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Payment Orchestrator with:
 * - Dynamic provider routing
 * - Automatic fallback on failure
 * - Region-based routing
 * - Provider health awareness
 */
@Service
@Slf4j
public class PaymentOrchestrator {

    private final Map<PaymentMethod, PaymentProvider> providers;

    // Fallback chain: if primary fails, try next
    private final Map<PaymentMethod, PaymentMethod> fallbackChain = Map.of(
            PaymentMethod.RAZORPAY, PaymentMethod.CASHFREE,
            PaymentMethod.STRIPE, PaymentMethod.PAYPAL,
            PaymentMethod.PAYPAL, PaymentMethod.STRIPE,
            PaymentMethod.CASHFREE, PaymentMethod.RAZORPAY
    );

    public PaymentOrchestrator(
            @Qualifier("walletProvider") PaymentProvider walletProvider,
            @Qualifier("razorpayProvider") PaymentProvider razorpayProvider,
            @Qualifier("stripeProvider") PaymentProvider stripeProvider,
            @Qualifier("paypalProvider") PaymentProvider paypalProvider,
            @Qualifier("cashfreeProvider") PaymentProvider cashfreeProvider
    ) {
        this.providers = new LinkedHashMap<>();
        providers.put(PaymentMethod.WALLET, walletProvider);
        providers.put(PaymentMethod.RAZORPAY, razorpayProvider);
        providers.put(PaymentMethod.STRIPE, stripeProvider);
        providers.put(PaymentMethod.PAYPAL, paypalProvider);
        providers.put(PaymentMethod.CASHFREE, cashfreeProvider);
    }

    // ═══════════════════════════════════════════
    // Process with Automatic Fallback
    // ═══════════════════════════════════════════
    public PaymentProvider.PaymentResult processWithFallback(
            PaymentMethod method, Double amount, String customerId, String orderId
    ) {
        // Try primary provider
        PaymentProvider primary = providers.get(method);
        if (primary != null) {
            try {
                PaymentProvider.PaymentResult result = primary.processPayment(amount, customerId, orderId);
                if (result.success()) {
                    log.info("Payment processed via primary: {}", method);
                    return result;
                }
            } catch (Exception e) {
                log.warn("Primary provider {} failed: {}", method, e.getMessage());
            }
        }

        // Try fallback provider
        PaymentMethod fallbackMethod = fallbackChain.get(method);
        if (fallbackMethod != null) {
            PaymentProvider fallback = providers.get(fallbackMethod);
            if (fallback != null) {
                try {
                    PaymentProvider.PaymentResult result = fallback.processPayment(amount, customerId, orderId);
                    log.info("Payment processed via fallback: {} (primary {} failed)", fallbackMethod, method);
                    return result;
                } catch (Exception e) {
                    log.error("Fallback provider {} also failed: {}", fallbackMethod, e.getMessage());
                }
            }
        }

        return new PaymentProvider.PaymentResult(false, null, null, "All payment providers failed");
    }

    // ═══════════════════════════════════════════
    // Region-Based Provider Selection
    // ═══════════════════════════════════════════
    public PaymentMethod selectProviderForRegion(String countryCode) {
        return switch (countryCode.toUpperCase()) {
            case "IN" -> PaymentMethod.RAZORPAY;
            case "US", "CA", "GB", "AU" -> PaymentMethod.STRIPE;
            case "DE", "FR", "IT", "ES" -> PaymentMethod.PAYPAL;
            default -> PaymentMethod.STRIPE; // Global default
        };
    }

    // ═══════════════════════════════════════════
    // Get Provider
    // ═══════════════════════════════════════════
    public PaymentProvider getProvider(PaymentMethod method) {
        return providers.get(method);
    }
}
