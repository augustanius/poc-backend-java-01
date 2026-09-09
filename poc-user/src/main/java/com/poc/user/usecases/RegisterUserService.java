package com.poc.user.usecases;

import com.poc.common.event.UserRegisteredEvent;
import com.poc.user.contracts.usecases.RegisterUserUseCase;
import com.poc.user.domain.model.User;
import com.poc.user.contracts.gateways.IdentityProvider;
import com.poc.user.contracts.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserService.class);

    private final IdentityProvider identityProvider;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RegisterUserService(IdentityProvider identityProvider,
                               UserRepository userRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.identityProvider = identityProvider;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public User register(String username, String email, String password) {
        var identityUser = identityProvider.createUser(username, email, password);
        try {
            var user = new User(identityUser.id(), identityUser.username(), identityUser.email());
            var saved = userRepository.save(user);
            eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId(), saved.getUsername(), saved.getEmail()));
            return saved;
        } catch (Exception e) {
            log.warn("DB save failed for user {}, rolling back identity provider creation", username, e);
            identityProvider.deleteUser(identityUser.id());
            throw e;
        }
    }
}
