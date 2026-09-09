package com.poc.user.contracts.usecases;

import com.poc.common.domain.PageResult;
import com.poc.user.domain.model.User;

import java.util.Optional;

public interface GetUserUseCase {

    Optional<User> findById(String id);

    PageResult<User> findAll(int page, int size);
}
