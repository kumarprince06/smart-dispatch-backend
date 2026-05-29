package com.smartdispatch.config;

import com.smartdispatch.auth.entity.Role;
import com.smartdispatch.auth.enums.RoleType;
import com.smartdispatch.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds role table on startup.
 * Runs FIRST (Order 1) — before AdminSeeder.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (RoleType roleType : RoleType.values()) {
            if (!roleRepository.existsByName(roleType)) {
                Role role = new Role();
                role.setName(roleType);
                roleRepository.save(role);
                log.info("Role seeded: {}", roleType);
            }
        }
    }
}
