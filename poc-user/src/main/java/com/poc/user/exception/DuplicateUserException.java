package com.poc.user.exception;

import com.poc.common.exception.ConflictException;

import java.util.Map;

public class DuplicateUserException extends ConflictException {

    public DuplicateUserException(String username) {
        super("User already exists: " + username, Map.of("username", username));
    }

    public DuplicateUserException(String username, String email) {
        super("User already exists with email: " + email,
                Map.of("username", username, "email", email));
    }
}
