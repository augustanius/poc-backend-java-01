package com.poc.user.domain.spi;

public interface IdentityProviderPort {

    record IdentityUser(String id, String username, String email) {}

    IdentityUser createUser(String username, String email, String password);

    void deleteUser(String userId);
}
