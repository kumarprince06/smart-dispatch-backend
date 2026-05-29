package com.smartdispatch.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private  String password;

    @NotBlank(message = "Phone number is required")
    private String phoneNo;

    @NotBlank(message =  "Role is required")
    private String role;

}
