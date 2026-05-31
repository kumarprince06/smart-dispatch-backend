package com.smartdispatch.payment.repository;

import com.smartdispatch.payment.entity.SavedPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, Long> {
    List<SavedPaymentMethod> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SavedPaymentMethod> findByUserIdAndIsDefaultTrue(Long userId);
    long countByUserId(Long userId);
}
