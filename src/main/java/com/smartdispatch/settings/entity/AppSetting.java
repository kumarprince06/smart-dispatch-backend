package com.smartdispatch.settings.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSetting {

    @Id
    @Column(name = "setting_key", nullable = false, unique = true)
    private String key;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean isPublic; // if true, driver and customer apps can read it

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
