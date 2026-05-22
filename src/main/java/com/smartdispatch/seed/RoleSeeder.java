package com.smartdispatch.seed;

import com.smartdispatch.auth.entity.Role;
import com.smartdispatch.auth.enums.RoleType;
import com.smartdispatch.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(1)
public class RoleSeeder implements CommandLineRunner {
    private final RoleRepository
            roleRepository;

    @Override
    public void run(String... args) {

        for (RoleType roleType
                : RoleType.values()) {

            boolean exists =
                    roleRepository
                            .existsByName(
                                    roleType
                            );

            if (!exists) {

                Role role = Role.builder()
                        .name(roleType)
                        .description(roleType.name() + " Role")
                        .build();

                roleRepository.save(role);
            }
        }
    }
}
