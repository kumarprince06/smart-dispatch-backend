package com.smartdispatch.notification.service;

import com.smartdispatch.notification.entity.Notification;
import com.smartdispatch.notification.enums.NotificationType;
import com.smartdispatch.notification.enums.NotificationTemplate;
import com.smartdispatch.notification.provider.NotificationProvider;
import com.smartdispatch.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Multi-channel notification service.
 * Dispatches to Email, SMS, Push, and In-App simultaneously.
 * Uses @Async for non-blocking delivery.
 */
@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Map<NotificationType, NotificationProvider> providers;

    public NotificationService(
            NotificationRepository notificationRepository,
            @Qualifier("emailProvider") NotificationProvider emailProvider,
            @Qualifier("smsProvider") NotificationProvider smsProvider,
            @Qualifier("pushProvider") NotificationProvider pushProvider
    ) {
        this.notificationRepository = notificationRepository;
        this.providers = Map.of(
                NotificationType.EMAIL, emailProvider,
                NotificationType.SMS, smsProvider,
                NotificationType.PUSH, pushProvider
        );
    }

    // ═══════════════════════════════════════════
    // Send via Template (preferred)
    // ═══════════════════════════════════════════
    @Async
    public void sendNotification(Long userId, String recipient, NotificationTemplate template,
                                  NotificationType type, String actionUrl, Object... args) {
        String subject = template.getSubject();
        String body = template.formatBody(args);

        // 1. Persist as in-app notification
        saveInAppNotification(userId, subject, body, actionUrl);

        // 2. Send via external channel
        sendViaProvider(type, recipient, subject, body);

        log.info("Notification sent → User: {}, Type: {}, Template: {}", userId, type, template);
    }

    // ═══════════════════════════════════════════
    // Send Multi-Channel (Email + SMS + Push at once)
    // ═══════════════════════════════════════════
    @Async
    public void sendMultiChannel(Long userId, String email, String phone,
                                  NotificationTemplate template, String actionUrl, Object... args) {
        String subject = template.getSubject();
        String body = template.formatBody(args);

        // Persist
        saveInAppNotification(userId, subject, body, actionUrl);

        // Send all channels
        sendViaProvider(NotificationType.EMAIL, email, subject, body);
        sendViaProvider(NotificationType.SMS, phone, subject, body);
        sendViaProvider(NotificationType.PUSH, userId.toString(), subject, body);

        log.info("Multi-channel notification sent → User: {}, Template: {}", userId, template);
    }

    // ═══════════════════════════════════════════
    // Send Custom (no template)
    // ═══════════════════════════════════════════
    @Async
    public void sendCustom(Long userId, String recipient, String title, String message,
                            NotificationType type, String actionUrl) {
        saveInAppNotification(userId, title, message, actionUrl);
        sendViaProvider(type, recipient, title, message);
    }

    // ═══════════════════════════════════════════
    // Get User Notifications (Inbox)
    // ═══════════════════════════════════════════
    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    // ═══════════════════════════════════════════
    // Get Unread Notifications
    // ═══════════════════════════════════════════
    public Page<Notification> getUnreadNotifications(Long userId, int page, int size) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    // ═══════════════════════════════════════════
    // Get Unread Count
    // ═══════════════════════════════════════════
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    // ═══════════════════════════════════════════
    // Mark as Read
    // ═══════════════════════════════════════════
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // ═══════════════════════════════════════════
    // Mark All as Read
    // ═══════════════════════════════════════════
    @Transactional
    public void markAllAsRead(Long userId) {
        Page<Notification> unread = notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, 100));
        unread.getContent().forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread.getContent());
    }

    // ═══════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════
    private void saveInAppNotification(Long userId, String title, String message, String actionUrl) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(NotificationType.IN_APP)
                .actionUrl(actionUrl)
                .build();
        notificationRepository.save(notification);
    }

    private void sendViaProvider(NotificationType type, String recipient, String subject, String body) {
        NotificationProvider provider = providers.get(type);
        if (provider != null) {
            try {
                provider.send(recipient, subject, body);
            } catch (Exception e) {
                log.error("Failed to send {} notification to {}: {}", type, recipient, e.getMessage());
            }
        }
    }
}
