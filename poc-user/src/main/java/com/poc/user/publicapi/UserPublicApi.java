package com.poc.user.publicapi;

import java.util.Optional;

public interface UserPublicApi {

    boolean userExists(String userId);

    Optional<UserInfo> findUserById(String userId);

    record UserInfo(String id, String username, String email) {}
}
