package com.poc.user.usecases;

import com.poc.common.domain.PageResult;
import com.poc.user.contracts.usecases.GetUserUseCase;
import com.poc.user.domain.model.User;
import com.poc.user.contracts.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetUserService implements GetUserUseCase {

    private final UserRepository userRepository;

    public GetUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<User> findAll(int page, int size) {
        return userRepository.findAll(page, size);
    }
}
