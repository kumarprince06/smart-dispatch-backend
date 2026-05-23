package com.smartdispatch.notification.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SMS provider placeholder.
 * Replace with: Twilio, AWS SNS, MSG91.
 */
@Component("smsProvider")
@Slf4j
public class SmsNotificationProvider implements NotificationProvider {

    @Override
    public void send(String to, String subject, String body) {
        // TODO: Replace with real SMS service (Twilio/MSG91/AWS SNS)
        log.info("[SMS] To: {} | Message: {}", to, body);
    }

    @Override
    public String getProviderName() {
        return "SMS";
    }
}
