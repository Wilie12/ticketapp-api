package com.nn.ticketapp_api.shared.identity.service;

import com.nn.ticketapp_api.shared.identity.config.IdentityProperties;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakIdentityGateway implements IdentityGateway {

    private final Keycloak keycloakClient;
    private final IdentityProperties properties;

    @Override
    @Cacheable(value = "identityCache", key = "#userId", unless = "#result == null")
    public Optional<String> getEmailById(UUID userId) {
        log.debug("Fetching email for user ID {} from Keycloak IAM", userId);

        try {
            String email = keycloakClient.realm(properties.realm())
                    .users()
                    .get(userId.toString())
                    .toRepresentation()
                    .getEmail();

            return Optional.ofNullable(email);
        } catch (NotFoundException e) {
            log.warn("User with ID {} not found in Keycloak", userId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to fetch user {} from Keycloak due to network or configuration error", userId, e);
            return Optional.empty();
        }
    }
}
