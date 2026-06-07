package com.smartdispatch.notification.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

/**
 * SMS provider supporting Fast2SMS for free real SMS delivery to India numbers.
 */
@Component("smsProvider")
@Slf4j
public class SmsNotificationProvider implements NotificationProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${FAST2SMS_API_KEY:}")
    private String fast2smsApiKey;

    @Override
    public void send(String to, String subject, String body) {
        log.info("[SMS] To: {} | Message: {}", to, body);

        if (fast2smsApiKey == null || fast2smsApiKey.trim().isEmpty()) {
            log.info("Fast2SMS API Key empty. Skipping real India SMS.");
            return;
        }

        try {
            // Clean phone number to 10-digit Indian format
            String cleanNumber = to.replaceAll("[^0-9]", "");
            if (cleanNumber.length() > 10) {
                cleanNumber = cleanNumber.substring(cleanNumber.length() - 10);
            }

            String url = "https://www.fast2sms.com/dev/bulkV2";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", fast2smsApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("route", "q");
            requestBody.put("message", body);
            requestBody.put("language", "english");
            requestBody.put("numbers", cleanNumber);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            restTemplate.postForEntity(url, request, String.class);

            log.info("Real SMS sent successfully to India mobile number +91{} via Fast2SMS.", cleanNumber);
        } catch (Exception e) {
            log.error("Failed to send real SMS via Fast2SMS: {}", e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "SMS";
    }
}
