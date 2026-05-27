package com.smartdispatch.payment.repository;

import com.smartdispatch.payment.entity.Payment;
import com.smartdispatch.payment.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.Map;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);

    Page<Payment> findByCustomerId(Long customerId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Long countByStatus(PaymentStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM Payment p WHERE p.status = 'SUCCESS'")
    Double calculateTotalRevenue();

    @org.springframework.data.jpa.repository.Query(value = "SELECT to_char(created_at, 'Dy') as name, COALESCE(SUM(amount), 0.0) as revenue, COUNT(*) as orders " +
            "FROM payments WHERE status = 'SUCCESS' AND created_at >= current_date - interval '6 days' " +
            "GROUP BY to_char(created_at, 'Dy'), created_at::date " +
            "ORDER BY created_at::date ASC", nativeQuery = true)
    List<Map<String, Object>> getRevenueChartData();
}
