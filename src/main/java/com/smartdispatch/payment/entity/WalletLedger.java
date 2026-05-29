package com.smartdispatch.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Double-entry accounting ledger.
 * NEVER update wallet balance directly — always create a ledger entry.
 * Balance = SUM(CREDIT) - SUM(DEBIT)
 */
@Entity
@Table(name = "wallet_ledger")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long walletId;

    private Long userId;

    private Double amount;

    private String type; // CREDIT, DEBIT

    private String source; // PAYMENT, REFUND, TOP_UP, CASHBACK, PROMO

    private String referenceId; // Order ID or Transaction ID

    private String description;

    private Double balanceAfter; // Snapshot of balance after this txn

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
