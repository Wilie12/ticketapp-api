package com.nn.ticketapp_api.shared.security;

import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String DEFAULT_ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(@NotNull Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);

        if (realmAccess == null || !realmAccess.containsKey(ROLES_CLAIM)) {
            return Collections.emptyList();
        }

        Object rolesObject = realmAccess.get(ROLES_CLAIM);

        if (rolesObject instanceof List<?> rawRoles) {
            return rawRoles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(roleName -> new SimpleGrantedAuthority(DEFAULT_ROLE_PREFIX + roleName))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
