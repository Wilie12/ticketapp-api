package com.nn.ticketapp_api.shared.security;

import com.nn.ticketapp_api.shared.security.KeycloakRealmRoleConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    @DisplayName("Should extract 'realm_access.roles' and map them with standard 'ROLE_' prefix")
    void shouldExtractRolesAndPrefixThemWithRole() {
        // given
        Jwt jwt = mock(Jwt.class);
        Map<String, Object> realmAccess = Map.of("roles", List.of("AGENT", "ADMIN"));

        given(jwt.getClaimAsMap("realm_access")).willReturn(realmAccess);

        // when
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // then
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_AGENT", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should return empty list of authorities when 'realm_access' claim is entirely missing")
    void shouldReturnEmptyListWhenRealmAccessClaimIsMissing() {
        // given
        Jwt jwt = mock(Jwt.class);
        given(jwt.getClaimAsMap("realm_access")).willReturn(Map.of());

        // when
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // then
        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list of authorities when 'roles' array inside 'realm_access' is missing")
    void shouldReturnEmptyListWhenRolesClaimIsMissing() {
        // given
        Jwt jwt = mock(Jwt.class);
        Map<String, Object> realmAccess = Map.of("other_claim", "value");
        given(jwt.getClaimAsMap("realm_access")).willReturn(realmAccess);

        // when
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // then
        assertThat(authorities).isEmpty();
    }
}
