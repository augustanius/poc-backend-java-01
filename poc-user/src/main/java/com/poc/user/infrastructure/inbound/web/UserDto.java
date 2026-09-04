package com.poc.user.infrastructure.inbound.web;

public class UserDto {

    public record CreateUserRequest(String username, String email) {}

    public record UserResponse(String id, String username, String email) {}
}
