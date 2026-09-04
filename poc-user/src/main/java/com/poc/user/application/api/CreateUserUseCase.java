package com.poc.user.application.api;

import com.poc.user.domain.model.User;

public interface CreateUserUseCase {

    User create(User user);
}
