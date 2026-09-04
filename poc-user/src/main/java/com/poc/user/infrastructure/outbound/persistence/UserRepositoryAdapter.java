package com.poc.user.infrastructure.outbound.persistence;

import com.poc.common.domain.PageResult;
import com.poc.user.domain.model.User;
import com.poc.user.domain.spi.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = new UserJpaEntity(user.getId(), user.getUsername(), user.getEmail());
        UserJpaEntity saved = userJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(String id) {
        return userJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public PageResult<User> findAll(int page, int size) {
        var result = userJpaRepository.findAll(PageRequest.of(page, size));
        return PageResult.of(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public void deleteById(String id) {
        userJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(String id) {
        return userJpaRepository.existsById(id);
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getUsername(), entity.getEmail());
    }
}
