package com.smartdispatch.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(String to, String resetToken) {
        String resetLink = "http://localhost:5173/reset-password?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Smart Dispatch - Password Reset");
        message.setText("Hello,\n\nYou have requested to reset your password. Please click the link below to set a new password:\n\n" 
                + resetLink + "\n\nIf you did not request this, please ignore this email.\n\nThanks,\nSmart Dispatch Team");

        try {
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
        }
    }

    public void sendEmailVerification(String to, String verificationToken) {
        String verifyLink = "http://localhost:5173/verify-email?token=" + verificationToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Smart Dispatch - Verify Your Email");
        message.setText("Hello,\n\nWelcome to Smart Dispatch! Please verify your email by clicking the link below:\n\n" 
                + verifyLink + "\n\nThanks,\nSmart Dispatch Team");

        try {
            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", to, e);
        }
    }
}
