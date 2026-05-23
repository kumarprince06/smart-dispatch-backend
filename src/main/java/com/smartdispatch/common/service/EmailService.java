package com.smartdispatch.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Email service placeholder.
 * Currently logs emails to console.
 * Replace with real SMTP implementation (spring-boot-starter-mail) when ready.
 */
@Service
@Slf4j
public class EmailService {

    public void sendPasswordResetEmail(String to, String resetToken) {
        // TODO: Replace with real email sending via JavaMailSender
        String resetLink = "http://localhost:3000/reset-password?token=" + resetToken;

        log.info("========== PASSWORD RESET EMAIL ==========");
        log.info("To: {}", to);
        log.info("Reset Link: {}", resetLink);
        log.info("Token: {}", resetToken);
        log.info("==========================================");
    }

    public void sendEmailVerification(String to, String verificationToken) {
        // TODO: Replace with real email sending via JavaMailSender
        String verifyLink = "http://localhost:3000/verify-email?token=" + verificationToken;

        log.info("========== EMAIL VERIFICATION ==========");
        log.info("To: {}", to);
        log.info("Verify Link: {}", verifyLink);
        log.info("Token: {}", verificationToken);
        log.info("========================================");
    }
}
