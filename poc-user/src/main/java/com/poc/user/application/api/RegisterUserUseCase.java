package com.poc.user.application.api;

import com.poc.user.domain.model.User;

public interface RegisterUserUseCase {

    User register(String username, String email, String password);
}
