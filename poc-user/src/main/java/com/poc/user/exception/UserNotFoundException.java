package com.poc.user.exception;

import com.poc.common.exception.NotFoundException;

import java.util.Map;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(String userId) {
        super("User not found: " + userId, Map.of("userId", userId));
    }

    public UserNotFoundException(String userId, String email) {
        super("User not found: " + email, Map.of("userId", userId, "email", email));
    }
}
