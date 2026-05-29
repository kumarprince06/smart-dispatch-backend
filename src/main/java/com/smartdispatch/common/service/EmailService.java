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

    public void sendDriverWelcomeEmail(String to, String firstName, String vehicleNumber) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Welcome to Smart Dispatch – Your Driver Account is Ready");
        message.setText(
            "Dear " + firstName + ",\n\n" +
            "Congratulations! You have been successfully onboarded as a driver on the Smart Dispatch platform.\n\n" +
            "Here are your account details:\n" +
            "  - Email: " + to + "\n" +
            "  - Temporary Password: Welcome@123\n" +
            "  - Registered Vehicle: " + vehicleNumber + "\n\n" +
            "Please log in using these credentials and change your password immediately.\n" +
            "Your account is currently pending KYC verification by our admin team.\n\n" +
            "Download the Smart Dispatch driver app to get started.\n\n" +
            "Best Regards,\n" +
            "Smart Dispatch Team"
        );

        try {
            mailSender.send(message);
            log.info("Driver welcome email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send driver welcome email to: {}", to, e);
        }
    }
}
