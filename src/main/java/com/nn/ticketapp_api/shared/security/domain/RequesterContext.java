package com.nn.ticketapp_api.shared.security.domain;

import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

public record RequesterContext(
        UUID userId,
        AccessLevel accessLevel
) {
    public boolean isInternal() {
        return accessLevel == AccessLevel.INTERNAL;
    }

    public void requireInternal() {
        if (!isInternal()) {
            throw new AccessDeniedException("Access Denied. Internal privileges required");
        }
    }
}
