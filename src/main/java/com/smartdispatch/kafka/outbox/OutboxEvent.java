package com.smartdispatch.kafka.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType; // e.g., "Order"

    @Column(nullable = false)
    private String aggregateId;   // e.g., order ID

    @Column(nullable = false)
    private String type;          // e.g., "OrderCreatedEvent"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;       // JSON serialized OrderEvent

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt; // null means pending

}
