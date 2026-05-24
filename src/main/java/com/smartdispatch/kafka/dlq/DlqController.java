package com.smartdispatch.kafka.dlq;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin-only REST controller for managing the Dead Letter Queue.
 *
 * Provides endpoints to:
 * - View all failed messages
 * - Get a count of unretried failures
 * - Replay a single failed message
 * - Replay all failed messages at once
 */
@RestController
@RequestMapping("/api/admin/dlq")
@RequiredArgsConstructor
@Tag(name = "Dead Letter Queue", description = "Admin endpoints to monitor and replay failed Kafka messages")
public class DlqController {

    private final DlqMonitorService dlqMonitorService;

    /**
     * GET /api/admin/dlq
     * Returns all DLQ records (most recent first).
     */
    @GetMapping
    @Operation(summary = "List all DLQ records", description = "Returns all failed Kafka messages currently in the DLQ monitor")
    public ResponseEntity<List<DlqRecord>> getAllDlqRecords() {
        return ResponseEntity.ok(dlqMonitorService.getAllDlqRecords());
    }

    /**
     * GET /api/admin/dlq/stats
     * Returns a quick summary of DLQ health.
     */
    @GetMapping("/stats")
    @Operation(summary = "DLQ statistics", description = "Returns count of total and unretried DLQ messages")
    public ResponseEntity<Map<String, Object>> getDlqStats() {
        List<DlqRecord> all = dlqMonitorService.getAllDlqRecords();
        return ResponseEntity.ok(Map.of(
                "totalDlqMessages", all.size(),
                "unretriedMessages", dlqMonitorService.getUnretriedCount(),
                "retriedMessages", all.size() - dlqMonitorService.getUnretriedCount()
        ));
    }

    /**
     * POST /api/admin/dlq/replay/{id}
     * Replays a single failed message back to its original topic.
     */
    @PostMapping("/replay/{id}")
    @Operation(summary = "Replay a DLQ message", description = "Re-publishes a failed message to its original Kafka topic for reprocessing")
    public ResponseEntity<Map<String, Object>> replayMessage(@PathVariable String id) {
        boolean success = dlqMonitorService.replayMessage(id);
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "status", "replayed",
                    "dlqRecordId", id
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "status", "failed",
                "message", "Record not found or already retried"
        ));
    }

    /**
     * POST /api/admin/dlq/replay-all
     * Replays ALL unretried DLQ messages.
     */
    @PostMapping("/replay-all")
    @Operation(summary = "Replay all DLQ messages", description = "Re-publishes all unretried failed messages back to their original topics")
    public ResponseEntity<Map<String, Object>> replayAll() {
        int count = dlqMonitorService.replayAll();
        return ResponseEntity.ok(Map.of(
                "status", "completed",
                "replayedCount", count
        ));
    }
}
