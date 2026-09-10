package com.poc.user.interfaceadapters.outbound.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.poc.user.contracts.gateways.IdentityProvider;
import com.poc.user.exception.KeycloakUnavailableException;
import com.poc.user.frameworks.config.KeycloakProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class KeycloakUserAdapter implements IdentityProvider {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserAdapter.class);

    private static final String DEFAULT_ROLE = "USER";

    private final RestClient restClient;
    private final KeycloakProperties properties;

    public KeycloakUserAdapter(RestClient restClient, KeycloakProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public IdentityUser createUser(String username, String email, String password) {
        String adminToken = getAdminToken();

        Map<String, Object> userPayload = Map.of(
                "username", username,
                "email", email,
                "firstName", username,
                "lastName", username,
                "emailVerified", true,
                "enabled", true,
                "requiredActions", List.of()
        );

        String createUrl = properties.authServerUrl() + "/admin/realms/" + properties.realm() + "/users";

        restClient.post()
                .uri(createUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(userPayload)
                .retrieve()
                .toBodilessEntity();

        String userId = findUserIdByUsername(username, adminToken);
        log.info("Created Keycloak user: id={}, username={}", userId, username);

        setUserPassword(userId, password, adminToken);
        assignRealmRole(userId, DEFAULT_ROLE, adminToken);

        return new IdentityUser(userId, username, email);
    }

    @SuppressWarnings("unchecked")
    private String findUserIdByUsername(String username, String adminToken) {
        String searchUrl = properties.authServerUrl() + "/admin/realms/" + properties.realm()
                + "/users?username=" + username;

        List<Map<String, Object>> results = restClient.get()
                .uri(searchUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(List.class);

        if (results == null || results.isEmpty()) {
            throw new KeycloakUnavailableException("Failed to find user: " + username);
        }
        return (String) results.getFirst().get("id");
    }

    private void setUserPassword(String userId, String password, String adminToken) {
        String resetUrl = properties.authServerUrl() + "/admin/realms/" + properties.realm()
                + "/users/" + userId + "/reset-password";

        restClient.put()
                .uri(resetUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "password", "value", password, "temporary", false))
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    private void assignRealmRole(String userId, String roleName, String adminToken) {
        String roleUrl = properties.authServerUrl() + "/admin/realms/" + properties.realm()
                + "/roles/" + roleName;

        Map<String, Object> role = restClient.get()
                .uri(roleUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .body(Map.class);

        if (role == null || role.get("id") == null) {
            throw new KeycloakUnavailableException("Failed to find role: " + roleName);
        }

        String mappingsUrl = properties.authServerUrl() + "/admin/realms/" + properties.realm()
                + "/users/" + userId + "/role-mappings/realm";

        restClient.post()
                .uri(mappingsUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(Map.of("id", role.get("id"), "name", roleName)))
                .retrieve()
                .toBodilessEntity();

        log.info("Assigned role {} to user: id={}", roleName, userId);
    }

    private String getAdminToken() {
        String tokenUrl = properties.authServerUrl() + "/realms/master/protocol/openid-connect/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", "admin-cli");
        formData.add("username", properties.adminUsername());
        formData.add("password", properties.adminPassword());
        formData.add("grant_type", "password");

        var response = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new KeycloakUnavailableException("Failed to obtain admin token");
        }
        return response.accessToken();
    }

    @Override
    public void deleteUser(String userId) {
        String adminToken = getAdminToken();
        String deleteUrl = properties.authServerUrl() + "/admin/realms/" + properties.realm()
                + "/users/" + userId;

        restClient.delete()
                .uri(deleteUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .toBodilessEntity();

        log.info("Deleted Keycloak user: id={}", userId);
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {}
}
