package com.poc.user.interfaceadapters.inbound.rest.dto;

public class AuthDto {

    public record RegisterRequest(String username, String email, String password) {}

    public record RegisterResponse(String id, String username, String email) {}
}
