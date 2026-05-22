package com.smartdispatch.auth.service;

import com.smartdispatch.auth.dto.RegisterRequest;
import com.smartdispatch.auth.entity.Role;
import com.smartdispatch.auth.entity.User;
import com.smartdispatch.auth.repository.RoleRepository;
import com.smartdispatch.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;


    // Register User
    public String register(RegisterRequest request){

        if (userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException(("Email is "));
        }

        Role role = roleRepository.findByName(request.getRole())
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
}
