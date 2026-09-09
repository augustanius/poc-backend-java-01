package com.poc.user.contracts.usecases;

import com.poc.user.domain.model.User;

public interface RegisterUserUseCase {

    User register(String username, String email, String password);
}
