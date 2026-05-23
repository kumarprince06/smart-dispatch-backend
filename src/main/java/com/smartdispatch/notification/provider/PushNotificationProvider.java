package com.smartdispatch.notification.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Push notification provider placeholder.
 * Replace with: Firebase Cloud Messaging (FCM), OneSignal.
 */
@Component("pushProvider")
@Slf4j
public class PushNotificationProvider implements NotificationProvider {

    @Override
    public void send(String to, String subject, String body) {
        // TODO: Replace with FCM/OneSignal
        log.info("[PUSH] To: {} | Title: {} | Body: {}", to, subject, body);
    }

    @Override
    public String getProviderName() {
        return "PUSH";
    }
}
