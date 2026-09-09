package com.poc.user.contracts.gateways;

public interface IdentityProvider {

    record IdentityUser(String id, String username, String email) {}

    IdentityUser createUser(String username, String email, String password);

    void deleteUser(String userId);
}
