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

    @PostMapping("/wallet/top-up")
    public ResponseEntity<ApiResponse<Double>> topUpWallet(@RequestParam Double amount) {
        Double balance = paymentService.topUpWallet(amount);
        return ResponseEntity.ok(ApiResponse.<Double>builder()
                .success(true).message("Wallet topped up. Balance: ₹" + balance).status(200).data(balance).build());
    }
}
