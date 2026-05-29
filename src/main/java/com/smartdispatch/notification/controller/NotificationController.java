package com.smartdispatch.notification.controller;

import com.smartdispatch.common.dto.ApiResponse;
import com.smartdispatch.notification.entity.Notification;
import com.smartdispatch.notification.service.NotificationService;
import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Notification>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = getCurrentUser();
        Page<Notification> notifications = notificationService.getUserNotifications(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.<Page<Notification>>builder()
                .success(true).message("Notifications fetched").status(200).data(notifications).build());
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Page<Notification>>> getUnread(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = getCurrentUser();
        Page<Notification> notifications = notificationService.getUnreadNotifications(user.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.<Page<Notification>>builder()
                .success(true).message("Unread notifications fetched").status(200).data(notifications).build());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        User user = getCurrentUser();
        Long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .success(true).message("Unread count").status(200).data(count).build());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Marked as read").status(200).build());
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        User user = getCurrentUser();
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("All marked as read").status(200).build());
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
