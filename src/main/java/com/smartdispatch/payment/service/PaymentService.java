package com.smartdispatch.payment.service;

import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.notification.enums.NotificationTemplate;
import com.smartdispatch.notification.enums.NotificationType;
import com.smartdispatch.notification.service.NotificationService;
import com.smartdispatch.order.entity.Order;
import com.smartdispatch.order.repository.OrderRepository;
import com.smartdispatch.payment.dto.PaymentRequest;
import com.smartdispatch.payment.dto.PaymentResponse;
import com.smartdispatch.payment.entity.Payment;
import com.smartdispatch.payment.entity.WalletLedger;
import com.smartdispatch.payment.enums.PaymentMethod;
import com.smartdispatch.payment.enums.PaymentStatus;
import com.smartdispatch.payment.orchestrator.PaymentOrchestrator;
import com.smartdispatch.payment.provider.PaymentProvider;
import com.smartdispatch.payment.repository.PaymentRepository;
import com.smartdispatch.payment.repository.WalletLedgerRepository;
import com.smartdispatch.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setProviderTransactionId(result.transactionId());
            payment.setPaidAt(LocalDateTime.now());

            // Update customer stats
            customer.setTotalSpent(customer.getTotalSpent() + order.getDeliveryFee().intValue());
            customer.setTotalOrders(customer.getTotalOrders() + 1);
            userRepository.save(customer);

            // Record ledger entry
            recordLedgerEntry(customer.getId(), order.getDeliveryFee(), "DEBIT",
                    "PAYMENT", order.getId().toString(), "Payment for order " + order.getTrackingNumber());

            // Notification
            notificationService.sendNotification(
                    customer.getId(), customer.getEmail(),
                    NotificationTemplate.PAYMENT_SUCCESS, NotificationType.EMAIL,
                    "/orders/" + order.getId(),
                    String.valueOf(order.getDeliveryFee()), order.getTrackingNumber()
            );

            log.info("Payment SUCCESS. TxnID: {}", payment.getTransactionId());
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
