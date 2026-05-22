package com.smartdispatch.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String message;
}
