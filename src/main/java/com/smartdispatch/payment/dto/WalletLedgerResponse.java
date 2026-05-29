package com.smartdispatch.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WalletLedgerResponse {
    private Long id;
    private Long userId;
    private Double amount;
    private String type;
    private String source;
    private String referenceId;
    private String description;
    private Double balanceAfter;
    private LocalDateTime createdAt;
}
