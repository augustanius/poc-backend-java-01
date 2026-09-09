package com.poc.notification.interfaceadapters.outbound.user;

import com.poc.notification.contracts.gateways.UserGateway;
import com.poc.user.publicapi.UserPublicApi;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserGatewayAdapter implements UserGateway {

    private final UserPublicApi userPublicApi;

    public UserGatewayAdapter(UserPublicApi userPublicApi) {
        this.userPublicApi = userPublicApi;
    }

    @Override
    public boolean userExists(String userId) {
        return userPublicApi.userExists(userId);
    }

    @Override
    public Optional<UserInfo> findUserById(String userId) {
        return userPublicApi.findUserById(userId)
                .map(info -> new UserInfo(info.id(), info.username(), info.email()));
    }
}
