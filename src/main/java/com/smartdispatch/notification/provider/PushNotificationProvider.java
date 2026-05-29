package com.smartdispatch.notification.provider;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Push notification provider using Firebase Cloud Messaging (FCM).
 */
@Component("pushProvider")
@Slf4j
public class PushNotificationProvider implements NotificationProvider {

    @Override
    public void send(String to, String subject, String body) {
        try {
            // Ignore if token is missing or if it's just the userId fallback string
            if (to == null || to.length() < 10) {
                log.warn("[PUSH SKIPPED] Invalid token format: {}", to);
                return;
            }

            Message message = Message.builder()
                    .setToken(to)
                    .setNotification(Notification.builder()
                            .setTitle(subject)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[PUSH SUCCESS] Message ID: {}", response);
        } catch (Exception e) {
            log.error("[PUSH FAILED] Failed to send push to token {}: {}", to, e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "PUSH";
    }
}
