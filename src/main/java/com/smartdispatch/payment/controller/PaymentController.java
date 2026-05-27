package com.smartdispatch.payment.controller;

import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.payment.dto.PaymentRequest;
import com.smartdispatch.payment.dto.PaymentResponse;
import com.smartdispatch.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request
    ) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<PaymentResponse>builder()
                        .success(true).message("Payment processed").status(201).data(response).build()
        );
    }

    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(@PathVariable Long orderId) {
        PaymentResponse response = paymentService.refundPayment(orderId);
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .success(true).message("Refund processed").status(200).data(response).build());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrder(@PathVariable Long orderId) {
        PaymentResponse response = paymentService.getPaymentByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .success(true).message("Payment fetched").status(200).data(response).build());
    }

    @GetMapping("/my-payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PaymentResponse> payments = paymentService.getMyPayments(page, size);
        return ResponseEntity.ok(ApiResponse.<Page<PaymentResponse>>builder()
                .success(true).message("Payments fetched").status(200).data(payments).build());
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PaymentResponse> payments = paymentService.getAllPayments(page, size);
        return ResponseEntity.ok(ApiResponse.<Page<PaymentResponse>>builder()
                .success(true).message("All payments fetched").status(200).data(payments).build());
    }

    @GetMapping("/admin/ledger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<com.smartdispatch.payment.dto.WalletLedgerResponse>>> getAllLedgerEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<com.smartdispatch.payment.dto.WalletLedgerResponse> entries = paymentService.getAllLedgerEntries(page, size);
        return ResponseEntity.ok(ApiResponse.<Page<com.smartdispatch.payment.dto.WalletLedgerResponse>>builder()
                .success(true).message("Ledger entries fetched").status(200).data(entries).build());
    }

    @PostMapping("/wallet/top-up")
    public ResponseEntity<ApiResponse<Double>> topUpWallet(@RequestParam Double amount) {
        Double balance = paymentService.topUpWallet(amount);
        return ResponseEntity.ok(ApiResponse.<Double>builder()
                .success(true).message("Wallet topped up. Balance: ₹" + balance).status(200).data(balance).build());
    }

    @GetMapping("/stats/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Double>> getTotalRevenue() {
        Double revenue = paymentService.getTotalRevenue();
        return ResponseEntity.ok(ApiResponse.<Double>builder()
                .success(true).message("Revenue fetched").status(200).data(revenue).build());
    }

    @GetMapping("/stats/chart")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getRevenueChartData() {
        java.util.List<java.util.Map<String, Object>> data = paymentService.getRevenueChartData();
        return ResponseEntity.ok(ApiResponse.<java.util.List<java.util.Map<String, Object>>>builder()
                .success(true).message("Chart data fetched").status(200).data(data).build());
    }
}
