package com.smartdispatch.config.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Singleton row ID
    @Column(unique = true)
    private String configKey; // e.g. "GLOBAL_SETTINGS"

    private Double platformFee;
    private Double taxRate;
    private Double baseFare;
    private Double perKmRate;
    
    private Boolean surgeEnabled;
    private Boolean autoAssign;

    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
