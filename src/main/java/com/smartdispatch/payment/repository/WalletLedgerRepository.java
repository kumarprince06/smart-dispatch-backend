package com.smartdispatch.payment.repository;

import com.smartdispatch.payment.entity.WalletLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {

    Page<WalletLedger> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(CASE WHEN l.type = 'CREDIT' THEN l.amount ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN l.type = 'DEBIT' THEN l.amount ELSE 0 END), 0) " +
            "FROM WalletLedger l WHERE l.userId = :userId")
    Double calculateBalanceFromLedger(Long userId);
}
