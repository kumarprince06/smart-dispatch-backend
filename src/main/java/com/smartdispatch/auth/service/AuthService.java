package com.smartdispatch.auth.service;

import com.smartdispatch.auth.dto.*;
import com.smartdispatch.auth.entity.PasswordResetToken;
import com.smartdispatch.auth.entity.RefreshToken;
import com.smartdispatch.auth.entity.Role;
import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.enums.RoleType;
import com.smartdispatch.auth.jwt.JwtService;
import com.smartdispatch.auth.repository.PasswordResetTokenRepository;
import com.smartdispatch.auth.repository.RefreshTokenRepository;
import com.smartdispatch.auth.repository.RoleRepository;
import com.smartdispatch.auth.repository.UserRepository;
import com.smartdispatch.auth.security.LoginRateLimiter;
import com.smartdispatch.common.service.EmailService;
import com.smartdispatch.exception.BadRequestException;
import com.smartdispatch.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final LoginRateLimiter loginRateLimiter;


    // Register User
    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered. Please login");
        }

        Role role = roleRepository.findByName(RoleType.valueOf(request.getRole()))
                .orElseThrow(() -> new BadRequestException("Role not found"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNo(request.getPhoneNo())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        // Send email verification
        String verificationToken = UUID.randomUUID().toString();
        emailService.sendEmailVerification(user.getEmail(), verificationToken);

        log.info("User registered successfully: {}", user.getEmail());

        return "User registered successfully";
    }

    // Login
    public LoginResponse login(LoginRequest request) {

        // Rate limiting check
        if (loginRateLimiter.isBlocked(request.getEmail())) {
            log.warn("Login blocked due to too many attempts: {}", request.getEmail());
            throw new BadRequestException(
                    "Too many failed attempts. Please try again after 15 minutes."
            );
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    loginRateLimiter.recordFailedAttempt(request.getEmail());
                    log.warn("Login failed. User not found: {}", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginRateLimiter.recordFailedAttempt(request.getEmail());
            log.warn("Login failed. Wrong password for: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        // Active check
        if (!user.getActive()) {
            throw new BadRequestException("Account is inactive. Please contact support.");
        }

        // Reset rate limiter on success
        loginRateLimiter.resetAttempts(request.getEmail());

        String accessToken = jwtService.generateToken(user.getEmail());

        String refreshTokenValue = jwtService.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        log.info("User logged in successfully: {}", user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNo())
                .refreshToken(refreshTokenValue)
                .build();
    }

    // Refresh Token
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (refreshToken.getRevoked()) {
            throw new BadRequestException("Token revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Refresh token expired");
        }

        String newAccessToken = jwtService.generateToken(
                refreshToken.getUser().getEmail()
        );

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    // Logout — Revoke Refresh Token
    public void logout(String refreshTokenValue) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BadRequestException("Token not found"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        log.info("User logged out. Refresh token revoked.");
    }

    // Change Password (for authenticated users)
    public void changePassword(ChangePasswordRequest request) {

        String email = SecurityUtil.getCurrentUserEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Validate new password matches confirm
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        // Ensure new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for: {}", email);
    }

    // Forgot Password — Generate reset token and send email
    public void forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No account found with this email"));

        String resetToken = UUID.randomUUID().toString();

        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .token(resetToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        passwordResetTokenRepository.save(passwordResetToken);

        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        log.info("Password reset token generated for: {}", user.getEmail());
    }

    // Reset Password — Validate token and set new password
    public void resetPassword(ResetPasswordRequest request) {

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (resetToken.getUsed()) {
            throw new BadRequestException("Reset token has already been used");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for: {}", user.getEmail());
    }

    // Verify Email
    public void verifyEmail(String token) {
        // TODO: Implement email verification token entity and lookup
        // For now, this is a placeholder that demonstrates the flow
        log.info("Email verification requested with token: {}", token);
        throw new BadRequestException(
                "Email verification is not fully implemented yet. Token: " + token
        );
    }
}

