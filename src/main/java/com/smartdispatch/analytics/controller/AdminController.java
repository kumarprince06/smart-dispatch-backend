package com.smartdispatch.analytics.controller;

import com.smartdispatch.analytics.dto.DashboardStats;
import com.smartdispatch.analytics.service.AnalyticsService;
import com.smartdispatch.audit.entity.AuditLog;
import com.smartdispatch.audit.service.AuditService;
import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.pricing.service.SurgePricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AnalyticsService analyticsService;
    private final AuditService auditService;
    private final SurgePricingService surgePricingService;

    // ═══════════════════════════════════════════
    // Dashboard Stats
    // ═══════════════════════════════════════════
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStats>> getDashboard() {
        DashboardStats stats = analyticsService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.<DashboardStats>builder()
                .success(true).message("Dashboard stats").status(200).data(stats).build());
    }

    // ═══════════════════════════════════════════
    // Surge Info
    // ═══════════════════════════════════════════
    @GetMapping("/surge")
    public ResponseEntity<ApiResponse<SurgePricingService.SurgeInfo>> getSurgeInfo() {
        SurgePricingService.SurgeInfo info = surgePricingService.getSurgeInfo();
        return ResponseEntity.ok(ApiResponse.<SurgePricingService.SurgeInfo>builder()
                .success(true).message("Surge info").status(200).data(info).build());
    }

    // ═══════════════════════════════════════════
    // Audit Logs
    // ═══════════════════════════════════════════
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AuditLog> logs = auditService.getAuditLogs(page, size);
        return ResponseEntity.ok(ApiResponse.<Page<AuditLog>>builder()
                .success(true).message("Audit logs").status(200).data(logs).build());
    }

    @GetMapping("/audit-logs/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getEntityAudit(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<AuditLog> logs = auditService.getEntityAudit(entityType, entityId, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<AuditLog>>builder()
                .success(true).message("Entity audit logs").status(200).data(logs).build());
    }
}
