package com.smartdispatch.auth.service;

import com.smartdispatch.auth.dto.*;
import com.smartdispatch.auth.entity.RefreshToken;
import com.smartdispatch.auth.entity.Role;
import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.enums.RoleType;
import com.smartdispatch.auth.jwt.JwtService;
import com.smartdispatch.auth.repository.RefreshTokenRepository;
import com.smartdispatch.auth.repository.RoleRepository;
import com.smartdispatch.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    // Register User
    public String register(RegisterRequest request){

        if (userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException(("Email is already registered. Please login"));
        }

        Role role = roleRepository.findByName(RoleType.valueOf(request.getRole()))
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNo(request.getPhoneNo())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        return "User registered successfully";
    }

    public LoginResponse login(LoginRequest request) {

//        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
//                    log.error("Login failed. User not found for email: {}", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

//            log.error("Login failed. Invalid password for email: {}", request.getEmail());

            throw new BadCredentialsException("Invalid email or password");
        }

        // Optional Active Check
        // if (!user.isActive()) {
        //     throw new RuntimeException("Account is inactive");
        // }

        String accessToken = jwtService.generateToken(user.getEmail());

        String refreshTokenValue =
                jwtService.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);


//        log.info("User logged in successfully: {}", user.getEmail());

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

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {

         RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
            .orElseThrow(() ->
                    new RuntimeException("Invalid refresh token"));

    if(refreshToken.getRevoked()) {
        throw new RuntimeException("Token revoked");
    }

    if(refreshToken.getExpiryDate()
            .isBefore(LocalDateTime.now())) {

        throw new RuntimeException("Refresh token expired");
    }

    String newAccessToken =
            jwtService.generateToken(
                    refreshToken.getUser().getEmail()
            );

    return RefreshTokenResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken.getToken())
            .build();
    }
}
