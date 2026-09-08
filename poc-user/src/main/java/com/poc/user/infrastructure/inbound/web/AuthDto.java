package com.poc.user.infrastructure.inbound.web;

public class AuthDto {

    public record RegisterRequest(String username, String email, String password) {}

    public record RegisterResponse(String id, String username, String email) {}
}
