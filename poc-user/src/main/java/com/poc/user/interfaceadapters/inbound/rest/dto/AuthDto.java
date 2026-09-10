package com.poc.user.interfaceadapters.inbound.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDto {

    public record RegisterRequest(
            @NotBlank(message = "Username is required") String username,
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format") String email,
            @NotBlank(message = "Password is required") String password
    ) {}

    public record RegisterResponse(String id, String username, String email) {}
}
