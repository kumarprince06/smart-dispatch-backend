package com.smartdispatch.payment.service;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.notification.enums.NotificationTemplate;
import com.smartdispatch.notification.enums.NotificationType;
import com.smartdispatch.notification.service.NotificationService;
import com.smartdispatch.order.entity.Order;
import com.smartdispatch.order.enums.OrderStatus;
import com.smartdispatch.order.repository.OrderRepository;
import com.smartdispatch.payment.dto.*;
import com.smartdispatch.payment.entity.Payment;
import com.smartdispatch.payment.entity.WalletLedger;
import com.smartdispatch.payment.enums.PaymentMethod;
import com.smartdispatch.payment.enums.PaymentStatus;
import com.smartdispatch.payment.orchestrator.PaymentOrchestrator;
import com.smartdispatch.payment.provider.PaymentProvider;
import com.smartdispatch.payment.provider.RazorpayPaymentProvider;
import com.smartdispatch.payment.repository.PaymentRepository;
import com.smartdispatch.payment.repository.WalletLedgerRepository;
import com.smartdispatch.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PaymentOrchestrator orchestrator;
    private final WalletLedgerRepository walletLedgerRepository;
    private final RazorpayPaymentProvider razorpayProvider;
    private final com.smartdispatch.payment.provider.StripePaymentProvider stripeProvider;
    private final com.smartdispatch.payment.provider.PayUPaymentProvider payuProvider;

    @Value("${payment.razorpay.key-id}")
    private String razorpayKeyId;

    // ═══════════════════════════════════════════
    // Get Available Providers
    // ═══════════════════════════════════════════
    public List<Map<String, String>> getAvailableProviders() {
        return List.of(
            Map.of("provider", "WALLET",   "displayName", "Fatafat Wallet"),
            Map.of("provider", "RAZORPAY", "displayName", "Razorpay (Cards / UPI / NetBanking)"),
            Map.of("provider", "PAYU", "displayName", "PayU (Cards / UPI)"),
            Map.of("provider", "STRIPE", "displayName", "Stripe (International Cards)"),
            Map.of("provider", "CASH_ON_DELIVERY", "displayName", "Cash on Delivery")
        );
    }

    // ═══════════════════════════════════════════
    // Create Payment Session (Step 1 for Gateway)
    // ═══════════════════════════════════════════
    @Transactional
    public PaymentSessionResponse createPaymentSession(PaymentSessionRequest request) {
        String email = SecurityUtil.getCurrentUserEmail();
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Customer not found"));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new BadRequestException("Order not found"));

        // Idempotency: don't create duplicate sessions
        String idempotencyKey = "PAY-" + order.getId() + "-" + customer.getId();
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Payment p = existing.get();
            return PaymentSessionResponse.builder()
                    .paymentId(p.getId())
                    .transactionId(p.getTransactionId())
                    .paymentSessionId(p.getProviderTransactionId())
                    .key(razorpayKeyId)
                    .orderId(p.getOrderId())
                    .amount(p.getAmount())
                    .currency("INR")
                    .provider(request.getProvider().name())
                    .build();
        }

        // Create internal payment record
        Payment payment = Payment.builder()
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .idempotencyKey(idempotencyKey)
                .orderId(order.getId())
                .customerId(customer.getId())
                .amount(order.getDeliveryFee())
                .currency("INR")
                .method(request.getProvider())
                .status(PaymentStatus.INITIATED)
                .build();

        if (request.getProvider() == PaymentMethod.CASH_ON_DELIVERY) {
            payment.setStatus(PaymentStatus.PENDING);
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            orderRepository.save(order);
            Payment saved = paymentRepository.save(payment);
            return PaymentSessionResponse.builder()
                    .paymentId(saved.getId()).transactionId(saved.getTransactionId())
                    .orderId(order.getId()).amount(order.getDeliveryFee())
                    .currency("INR").provider("CASH_ON_DELIVERY").build();
        }

        PaymentProvider.PaymentResult result;
        String publicKey = null;
        
        if (request.getProvider() == PaymentMethod.STRIPE) {
            result = stripeProvider.processPayment(
                    order.getDeliveryFee(), customer.getId().toString(), order.getId().toString()
            );
            publicKey = result.paymentUrl();
        } else if (request.getProvider() == PaymentMethod.PAYU) {
            result = payuProvider.processPayment(
                    order.getDeliveryFee(), customer.getId().toString(), order.getId().toString()
            );
            publicKey = result.paymentUrl(); // Same as Stripe, PayU URL is in paymentUrl
        } else {
            // Default to Razorpay
            result = razorpayProvider.processPayment(
                    order.getDeliveryFee(), customer.getId().toString(), order.getId().toString()
            );
            publicKey = razorpayKeyId;
        }

        payment.setProviderTransactionId(result.transactionId()); 
        if (result.paymentUrl() != null) {
            payment.setPaymentUrl(result.paymentUrl());
        }
        payment.setStatus(PaymentStatus.PROCESSING);
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);
        Payment saved = paymentRepository.save(payment);

        log.info("[PAYMENT SESSION] Created. TxnID: {}, GatewayOrderId: {}",
                saved.getTransactionId(), result.transactionId());

        return PaymentSessionResponse.builder()
                .paymentId(saved.getId())
                .transactionId(saved.getTransactionId())
                .paymentSessionId(result.transactionId()) 
                .key(publicKey)                         
                .orderId(order.getId())
                .amount(order.getDeliveryFee())
                .currency("INR")
                .provider(request.getProvider().name())
                .paymentUrl(result.paymentUrl())
                .build();
    }

    // ═══════════════════════════════════════════
    // Verify Payment (after frontend checkout)
    // ═══════════════════════════════════════════
    @Transactional
    public PaymentResponse verifyPayment(PaymentVerifyRequest request) {
        // Find payment by razorpay order_id (stored as providerTransactionId)
        Payment payment = paymentRepository.findByProviderTransactionId(request.getRazorpayOrderId())
                .orElseThrow(() -> new BadRequestException("Payment session not found"));

        // Verify the signature
        boolean valid = razorpayProvider.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid payment signature");
            paymentRepository.save(payment);
            throw new BadRequestException("Payment signature verification failed");
        }

        // Signature valid — mark as SUCCESS (webhook will also confirm)
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayPaymentId(request.getRazorpayPaymentId());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Update order status to CONFIRMED
        orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("[VERIFY] Order {} confirmed after signature check.", order.getId());
        });

        log.info("[VERIFY] Payment {} verified successfully.", payment.getTransactionId());
        return mapToResponse(payment);
    }

    // ═══════════════════════════════════════════
    // Handle Webhook (Source of Truth)
    // ═══════════════════════════════════════════
    @Transactional
    public void handleRazorpayWebhook(String razorpayPaymentId, String event) {
        paymentRepository.findByGatewayPaymentId(razorpayPaymentId).ifPresent(payment -> {
            if ("payment.captured".equals(event)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);

                // Final order confirmation via webhook
                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    order.setStatus(OrderStatus.CONFIRMED);
                    orderRepository.save(order);
                });

                // Update customer stats
                userRepository.findById(payment.getCustomerId()).ifPresent(customer -> {
                    customer.setTotalSpent(customer.getTotalSpent() + payment.getAmount().intValue());
                    customer.setTotalOrders(customer.getTotalOrders() + 1);
                    userRepository.save(customer);
                    recordLedgerEntry(customer.getId(), payment.getAmount(), "DEBIT",
                            "PAYMENT", payment.getOrderId().toString(),
                            "Payment for order #" + payment.getOrderId());
                });

                log.info("[WEBHOOK] payment.captured confirmed. TxnID: {}", payment.getTransactionId());

            } else if ("payment.failed".equals(event)) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                    order.setStatus(OrderStatus.PAYMENT_FAILED);
                    orderRepository.save(order);
                });
                log.warn("[WEBHOOK] payment.failed. TxnID: {}", payment.getTransactionId());
            }
        });
    }

    // ═══════════════════════════════════════════
    // Process Payment (with Idempotency)
    // ═══════════════════════════════════════════
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        String email = SecurityUtil.getCurrentUserEmail();
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Customer not found"));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new BadRequestException("Order not found"));

        // IDEMPOTENCY CHECK: same key = same result
        String idempotencyKey = "PAY-" + order.getId() + "-" + customer.getId();
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.warn("Idempotent request detected. Returning existing payment: {}", existing.get().getTransactionId());
            return mapToResponse(existing.get());
        }

        // Create payment record
        Payment payment = Payment.builder()
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .idempotencyKey(idempotencyKey)
                .orderId(order.getId())
                .customerId(customer.getId())
                .amount(order.getDeliveryFee())
                .currency("INR")
                .method(request.getMethod())
                .status(PaymentStatus.INITIATED)
                .build();

        // COD — no provider needed
        if (request.getMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            payment.setStatus(PaymentStatus.PENDING);
            Payment saved = paymentRepository.save(payment);
            log.info("COD payment created. TxnID: {}", saved.getTransactionId());
            return mapToResponse(saved);
        }

        // Process via orchestrator (with automatic fallback)
        payment.setStatus(PaymentStatus.PROCESSING);

        PaymentProvider.PaymentResult result = orchestrator.processWithFallback(
                request.getMethod(), order.getDeliveryFee(),
                customer.getId().toString(), order.getId().toString()
        );

        if (result.success()) {
            // For gateway payments (Razorpay etc): keep status PENDING until webhook confirms.
            // For Wallet: mark SUCCESS immediately since deduction is instant.
            boolean isGatewayPayment = (request.getMethod() != PaymentMethod.WALLET);
            if (isGatewayPayment) {
                payment.setStatus(PaymentStatus.PENDING);
            } else {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(LocalDateTime.now());
                customer.setTotalSpent(customer.getTotalSpent() + order.getDeliveryFee().intValue());
                customer.setTotalOrders(customer.getTotalOrders() + 1);
                userRepository.save(customer);
                recordLedgerEntry(customer.getId(), order.getDeliveryFee(), "DEBIT",
                        "PAYMENT", order.getId().toString(), "Payment for order " + order.getTrackingNumber());
                notificationService.sendNotification(
                        customer.getId(), customer.getEmail(),
                        NotificationTemplate.PAYMENT_SUCCESS, NotificationType.EMAIL,
                        "/orders/" + order.getId(),
                        String.valueOf(order.getDeliveryFee()), order.getTrackingNumber()
                );
            }

            payment.setProviderTransactionId(result.transactionId());
            // Store the payment URL for gateway redirect (e.g., Razorpay Payment Link)
            if (result.paymentUrl() != null) {
                payment.setPaymentUrl(result.paymentUrl());
            }

            log.info("Payment initiated. Status: {}, TxnID: {}, URL: {}",
                    payment.getStatus(), payment.getTransactionId(), result.paymentUrl());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(result.failureReason());
            payment.setRetryCount(payment.getRetryCount() + 1);

            notificationService.sendNotification(
                    customer.getId(), customer.getEmail(),
                    NotificationTemplate.PAYMENT_FAILED, NotificationType.EMAIL,
                    "/orders/" + order.getId(),
                    String.valueOf(order.getDeliveryFee()), order.getTrackingNumber()
            );

            log.warn("Payment FAILED. TxnID: {}, Reason: {}", payment.getTransactionId(), result.failureReason());
        }

        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }

    // ═══════════════════════════════════════════
    // Refund Payment
    // ═══════════════════════════════════════════
    @Transactional
    public PaymentResponse refundPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BadRequestException("Payment not found for order: " + orderId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Only successful payments can be refunded");
        }

        if (payment.getMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            throw new BadRequestException("COD payments cannot be refunded online");
        }

        PaymentProvider provider = orchestrator.getProvider(payment.getMethod());
        PaymentProvider.PaymentResult result = provider.processRefund(
                payment.getProviderTransactionId(), payment.getAmount()
        );

        if (result.success()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());

            // Record ledger entry for refund
            recordLedgerEntry(payment.getCustomerId(), payment.getAmount(), "CREDIT",
                    "REFUND", orderId.toString(), "Refund for order " + orderId);

            notificationService.sendNotification(
                    payment.getCustomerId(), payment.getCustomerId().toString(),
                    NotificationTemplate.PAYMENT_REFUND, NotificationType.PUSH,
                    "/orders/" + orderId,
                    String.valueOf(payment.getAmount()), orderId.toString()
            );
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Refund processed. TxnID: {}", payment.getTransactionId());
        return mapToResponse(saved);
    }

    // ═══════════════════════════════════════════
    // Get Payment by Order
    // ═══════════════════════════════════════════
    public PaymentResponse getPaymentByOrder(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BadRequestException("Payment not found"));
        return mapToResponse(payment);
    }

    // ═══════════════════════════════════════════
    // Get My Payments
    // ═══════════════════════════════════════════
    public Page<PaymentResponse> getMyPayments(int page, int size) {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return paymentRepository.findByCustomerId(user.getId(),
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::mapToResponse);
    }

    // ═══════════════════════════════════════════
    // Get All Payments (Admin)
    // ═══════════════════════════════════════════
    public Page<PaymentResponse> getAllPayments(int page, int size) {
        return paymentRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::mapToResponse);
    }

    // ═══════════════════════════════════════════
    // Get All Ledger Entries (Admin)
    // ═══════════════════════════════════════════
    public Page<com.smartdispatch.payment.dto.WalletLedgerResponse> getAllLedgerEntries(int page, int size) {
        return walletLedgerRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(l -> com.smartdispatch.payment.dto.WalletLedgerResponse.builder()
                        .id(l.getId())
                        .userId(l.getUserId())
                        .amount(l.getAmount())
                        .type(l.getType())
                        .source(l.getSource())
                        .referenceId(l.getReferenceId())
                        .description(l.getDescription())
                        .balanceAfter(l.getBalanceAfter())
                        .createdAt(l.getCreatedAt())
                        .build());
    }

    // ═══════════════════════════════════════════
    // Get My Ledger Entries (Wallet Transactions)
    // ═══════════════════════════════════════════
    public Page<com.smartdispatch.payment.dto.WalletLedgerResponse> getMyLedgerEntries(int page, int size) {
        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return walletLedgerRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
                .map(l -> com.smartdispatch.payment.dto.WalletLedgerResponse.builder()
                        .id(l.getId())
                        .userId(l.getUserId())
                        .amount(l.getAmount())
                        .type(l.getType())
                        .source(l.getSource())
                        .referenceId(l.getReferenceId())
                        .description(l.getDescription())
                        .balanceAfter(l.getBalanceAfter())
                        .createdAt(l.getCreatedAt())
                        .build());
    }

    // ═══════════════════════════════════════════
    // Wallet Top-Up
    // ═══════════════════════════════════════════
    @Transactional
    public Double topUpWallet(Double amount) {
        if (amount <= 0) throw new BadRequestException("Amount must be positive");

        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setWalletBalance(user.getWalletBalance() + amount);
        userRepository.save(user);

        // Record ledger
        recordLedgerEntry(user.getId(), amount, "CREDIT", "TOP_UP",
                null, "Wallet top-up");

        notificationService.sendNotification(
                user.getId(), user.getEmail(),
                NotificationTemplate.WALLET_CREDITED, NotificationType.EMAIL,
                null, String.valueOf(amount), String.valueOf(user.getWalletBalance())
        );

        log.info("Wallet topped up. User: {}, Amount: ₹{}, Balance: ₹{}", email, amount, user.getWalletBalance());
        return user.getWalletBalance();
    }

    // ═══════════════════════════════════════════
    // PRIVATE: Ledger Entry (Double-Entry Accounting)
    // ═══════════════════════════════════════════
    private void recordLedgerEntry(Long userId, Double amount, String type,
                                    String source, String referenceId, String description) {
        WalletLedger ledger = WalletLedger.builder()
                .userId(userId)
                .amount(amount)
                .type(type)
                .source(source)
                .referenceId(referenceId)
                .description(description)
                .build();
        walletLedgerRepository.save(ledger);
        log.debug("Ledger: {} {} ₹{} for user {}", type, source, amount, userId);
    }

    private PaymentResponse mapToResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getId())
                .transactionId(p.getTransactionId())
                .orderId(p.getOrderId())
                .amount(p.getAmount())
                .method(p.getMethod())
                .status(p.getStatus())
                .providerTransactionId(p.getProviderTransactionId())
                .paymentUrl(p.getPaymentUrl())
                .failureReason(p.getFailureReason())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }

    public Double getTotalRevenue() {
        return paymentRepository.calculateTotalRevenue();
    }

    public java.util.List<java.util.Map<String, Object>> getRevenueChartData() {
        return paymentRepository.getRevenueChartData();
    }
}
