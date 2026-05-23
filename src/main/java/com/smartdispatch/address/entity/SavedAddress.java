package com.smartdispatch.address.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_addresses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false)
    private String label; // HOME, WORK, CUSTOM

    @Column(nullable = false)
    private String fullAddress;

    private String landmark;

    private Double latitude;

    private Double longitude;

    private String contactName;

    private String contactPhone;

    @Builder.Default
    private Boolean isDefault = false;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
