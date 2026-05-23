package com.smartdispatch.notification.provider;

/**
 * Strategy pattern interface.
 * Each notification channel implements this.
 * Easily swap providers: Twilio → AWS SNS, SendGrid → SES.
 */
public interface NotificationProvider {

    void send(String to, String subject, String body);

    String getProviderName();
}
