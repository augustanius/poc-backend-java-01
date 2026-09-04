package com.poc.user.domain.spi;

import com.poc.common.domain.PageResult;
import com.poc.user.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(String id);

    PageResult<User> findAll(int page, int size);

    void deleteById(String id);

    boolean existsById(String id);
}
