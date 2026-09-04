package com.poc.user.application.service;

import com.poc.user.application.api.CreateUserUseCase;
import com.poc.user.domain.model.User;
import com.poc.user.domain.spi.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;

    public CreateUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User create(User user) {
        return userRepository.save(user);
    }
}
