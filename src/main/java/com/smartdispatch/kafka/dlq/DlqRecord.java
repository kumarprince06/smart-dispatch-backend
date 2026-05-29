package com.smartdispatch.kafka.dlq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single message that failed processing and was
 * routed to the Dead Letter Queue.
 *
 * This is a POJO (not a JPA entity) because we store DLQ records
 * in-memory for simplicity. In production, you'd persist this to
 * a dedicated database table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqRecord {

    /** Unique identifier: "{partition}-{offset}" */
    private String id;

    /** The original topic this message was meant for */
    private String originalTopic;

    /** The Kafka message key (e.g., orderId) */
    private String key;

    /** The serialized payload that failed processing */
    private String payload;

    /** The partition in the DLQ topic */
    private int partition;

    /** The offset in the DLQ topic */
    private long offset;

    /** When the message was routed to DLQ */
    private LocalDateTime failedAt;

    /** Whether this message has been replayed */
    private boolean retried;

    /** When this message was replayed */
    private LocalDateTime retriedAt;
}
