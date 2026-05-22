package com.smartdispatch.seed;

import com.smartdispatch.auth.entity.Role;
import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.enums.RoleType;
import com.smartdispatch.auth.repository.RoleRepository;
import com.smartdispatch.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(2)
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String...args){
        boolean exists = userRepository.existsByEmail("super.admin@yopmail.com");

        if(!exists){

            Role adminRole = roleRepository
                    .findByName(RoleType.ADMIN)
                    .orElseThrow(() ->
                            new RuntimeException("ADMIN role not found"));

            User admin = new User();
            admin.setFirstName("Kumar");
            admin.setLastName("Prince");
            admin.setEmail("super.admin@yopmail.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole(adminRole);
            admin.setPhoneNo("8617266822");

            userRepository.save(admin);
        }
    }
}
