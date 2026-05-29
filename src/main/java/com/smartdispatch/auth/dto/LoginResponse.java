package com.smartdispatch.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String role;
}
