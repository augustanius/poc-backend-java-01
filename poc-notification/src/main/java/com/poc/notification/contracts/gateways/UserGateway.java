package com.poc.notification.contracts.gateways;

import java.util.Optional;

public interface UserGateway {

    boolean userExists(String userId);

    Optional<UserInfo> findUserById(String userId);

    record UserInfo(String id, String username, String email) {}
}
