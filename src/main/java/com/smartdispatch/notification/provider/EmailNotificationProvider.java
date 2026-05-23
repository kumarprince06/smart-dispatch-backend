package com.smartdispatch.notification.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Email provider placeholder.
 * Replace with: SendGrid, AWS SES, or JavaMailSender.
 */
@Component("emailProvider")
@Slf4j
public class EmailNotificationProvider implements NotificationProvider {

    @Override
    public void send(String to, String subject, String body) {
        // TODO: Replace with real email service (SendGrid/SES/SMTP)
        log.info("[EMAIL] To: {} | Subject: {} | Body: {}", to, subject, body);
    }

    @Override
    public String getProviderName() {
        return "EMAIL";
    }
}
