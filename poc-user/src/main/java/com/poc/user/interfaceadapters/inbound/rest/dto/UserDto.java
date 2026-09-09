package com.poc.user.interfaceadapters.inbound.rest.dto;

public class UserDto {

    public record CreateUserRequest(String username, String email) {}

    public record UserResponse(String id, String username, String email) {}
}
