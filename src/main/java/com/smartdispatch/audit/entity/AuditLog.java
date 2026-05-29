package com.smartdispatch.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Generic audit log for all state changes across the system.
 * Tracks: who did what, when, on which entity.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType; // ORDER, DRIVER, PAYMENT, USER

    private Long entityId;

    private String action; // CREATED, UPDATED, DELETED, STATUS_CHANGED

    private String previousValue;

    private String newValue;

    private String performedBy; // email of the user

    private String ipAddress;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
