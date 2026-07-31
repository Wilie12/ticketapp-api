package com.nn.ticketapp_api.shared.security.domain;

import java.util.UUID;

public record RequesterContext(
        UUID userId,
        AccessLevel accessLevel
) {
    public boolean isInternal() {
        return accessLevel == AccessLevel.INTERNAL;
    }
}
