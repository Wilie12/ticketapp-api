package com.nn.ticketapp_api.shared.identity.service;

import java.util.Optional;
import java.util.UUID;

public interface IdentityGateway {
    Optional<String> getEmailById(UUID userId);
}
