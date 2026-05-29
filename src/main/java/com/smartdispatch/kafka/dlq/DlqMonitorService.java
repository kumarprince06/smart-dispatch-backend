package com.smartdispatch.kafka.dlq;

import com.smartdispatch.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Dead Letter Queue Monitor Service.
 *
 * This service listens to the ".DLQ" topic and stores failed messages
 * in-memory so that admins can inspect them via the DlqController.
 *
 * It also provides the ability to REPLAY a failed message back to the
 * original topic for re-processing (e.g., after a bug fix or infra recovery).
 *
 * In a production environment, you would persist these to a database table
 * (e.g., "dlq_events") instead of in-memory. For our project, this
 * demonstrates the pattern clearly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DlqMonitorService {

    // In-memory store of failed messages (bounded to last 100)
    private static final int MAX_DLQ_RECORDS = 100;
    private final ConcurrentLinkedDeque<DlqRecord> dlqRecords = new ConcurrentLinkedDeque<>();

    private final KafkaTemplate<String, Object> dlqKafkaTemplate;

    /**
     * Kafka listener that consumes from the DLQ topic.
     * Every message that lands here has already failed 3 retries.
     */
    @KafkaListener(
            topics = KafkaConfig.ORDER_EVENTS_DLQ_TOPIC,
            groupId = "dlq-monitor-group"
    )
    public void consumeDlqMessage(ConsumerRecord<String, Object> record) {
        log.error("🔴 DLQ Message received | Topic: {} | Key: {} | Partition: {} | Offset: {}",
                record.topic(), record.key(), record.partition(), record.offset());

        DlqRecord dlqRecord = DlqRecord.builder()
                .id(record.partition() + "-" + record.offset())
                .originalTopic(record.topic().replace(".DLQ", ""))
                .key(record.key())
                .payload(record.value() != null ? record.value().toString() : "null")
                .partition(record.partition())
                .offset(record.offset())
                .failedAt(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault()))
                .retried(false)
                .build();

        dlqRecords.addFirst(dlqRecord);

        // Keep only the last MAX_DLQ_RECORDS
        while (dlqRecords.size() > MAX_DLQ_RECORDS) {
            dlqRecords.removeLast();
        }
    }

    /**
     * Get all DLQ records for admin inspection.
     */
    public List<DlqRecord> getAllDlqRecords() {
        return Collections.unmodifiableList(new ArrayList<>(dlqRecords));
    }

    /**
     * Get count of unretried DLQ records.
     */
    public long getUnretriedCount() {
        return dlqRecords.stream().filter(r -> !r.isRetried()).count();
    }

    /**
     * Replay a specific DLQ message back to the original topic.
     * This is used after a bug fix or infrastructure recovery to
     * re-process a previously failed message.
     */
    public boolean replayMessage(String dlqRecordId) {
        for (DlqRecord record : dlqRecords) {
            if (record.getId().equals(dlqRecordId) && !record.isRetried()) {
                try {
                    dlqKafkaTemplate.send(record.getOriginalTopic(), record.getKey(), record.getPayload());
                    record.setRetried(true);
                    record.setRetriedAt(LocalDateTime.now());
                    log.info("✅ DLQ message replayed successfully. ID: {}, Topic: {}",
                            dlqRecordId, record.getOriginalTopic());
                    return true;
                } catch (Exception e) {
                    log.error("Failed to replay DLQ message {}: {}", dlqRecordId, e.getMessage());
                    return false;
                }
            }
        }
        log.warn("DLQ record not found or already retried: {}", dlqRecordId);
        return false;
    }

    /**
     * Replay ALL unretried DLQ messages back to their original topics.
     */
    public int replayAll() {
        int count = 0;
        for (DlqRecord record : dlqRecords) {
            if (!record.isRetried()) {
                if (replayMessage(record.getId())) {
                    count++;
                }
            }
        }
        log.info("Replayed {} DLQ messages", count);
        return count;
    }
}
