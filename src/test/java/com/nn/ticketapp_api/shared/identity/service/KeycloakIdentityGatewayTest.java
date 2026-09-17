package com.nn.ticketapp_api.shared.identity.service;

import com.nn.ticketapp_api.BaseIntegrationTest;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

public class KeycloakIdentityGatewayTest extends BaseIntegrationTest {

    @Autowired
    private IdentityGateway identityGateway;
    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private Keycloak keycloakClient;

    private UsersResource usersResource;

    @BeforeEach
    void setup() {
        Objects.requireNonNull(cacheManager.getCache("identityCache")).clear();

        RealmResource realmResource = mock(RealmResource.class);
        usersResource = mock(UsersResource.class);

        given(keycloakClient.realm(anyString())).willReturn(realmResource);
        given(realmResource.users()).willReturn(usersResource);
    }

    @Test
    @DisplayName("Should retrieve email from Keycloak and actively cache subsequent requests")
    void shouldFetchEmailAndCacheResult() {
        // given
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UserResource userResource = mock(UserResource.class);
        UserRepresentation userRepresentation = mock(UserRepresentation.class);

        given(usersResource.get(userId.toString())).willReturn(userResource);
        given(userResource.toRepresentation()).willReturn(userRepresentation);
        given(userRepresentation.getEmail()).willReturn("test@ticketapp.local");

        // when
        Optional<String> firstCall = identityGateway.getEmailById(userId);
        Optional<String> secondCall = identityGateway.getEmailById(userId);

        // then
        assertThat(firstCall).isPresent().contains("test@ticketapp.local");
        assertThat(secondCall).isPresent().contains("test@ticketapp.local");

        then(usersResource).should(times(1)).get(userId.toString());
    }

    @Test
    @DisplayName("Should return empty Optional and NOT cache empty results when user is not found")
    void shouldReturnEmptyWhenUserNotFound() {
        // given
        UUID missingUserId = UUID.randomUUID();
        given(usersResource.get(missingUserId.toString())).willThrow(new NotFoundException("Not found"));

        // when
        Optional<String> firstCall = identityGateway.getEmailById(missingUserId);
        Optional<String> secondCall = identityGateway.getEmailById(missingUserId);

        // then
        assertThat(firstCall).isEmpty();
        assertThat(secondCall).isEmpty();

        then(usersResource).should(times(2)).get(missingUserId.toString());
    }
}
