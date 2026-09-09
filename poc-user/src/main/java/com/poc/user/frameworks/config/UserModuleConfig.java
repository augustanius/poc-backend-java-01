package com.poc.user.frameworks.config;

import com.poc.user.contracts.gateways.IdentityProvider;
import com.poc.user.interfaceadapters.outbound.keycloak.KeycloakUserAdapter;
import com.poc.user.publicapi.UserPublicApi;
import com.poc.user.usecases.GetUserService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class UserModuleConfig {

    @Bean
    public IdentityProvider identityProvider(KeycloakProperties properties) {
        return new KeycloakUserAdapter(RestClient.create(), properties);
    }

    @Bean
    public UserPublicApi userPublicApi(GetUserService getUserService) {
        return new UserPublicApi() {
            @Override
            public boolean userExists(String userId) {
                return getUserService.findById(userId).isPresent();
            }

            @Override
            public java.util.Optional<UserInfo> findUserById(String userId) {
                return getUserService.findById(userId)
                        .map(u -> new UserInfo(u.getId(), u.getUsername(), u.getEmail()));
            }
        };
    }
}
