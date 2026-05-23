package com.smartdispatch.config;

import com.smartdispatch.auth.entity.Role;
import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.enums.RoleType;
import com.smartdispatch.auth.repository.RoleRepository;
import com.smartdispatch.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds admin user on startup.
 * Runs AFTER RoleSeeder (Order 2).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "super.admin@yopmail.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByName(RoleType.ADMIN)
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found. RoleSeeder must run first."));

            User admin = User.builder()
                    .firstName("Kumar")
                    .lastName("Prince")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(adminRole)
                    .emailVerified(true)
                    .active(true)
                    .build();

            userRepository.save(admin);
            log.info("Admin user seeded: {}", adminEmail);
        }
    }
}
