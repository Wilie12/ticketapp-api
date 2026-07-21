package com.nn.ticketapp_api.communication.api.response;

import com.nn.ticketapp_api.communication.domain.CommunicationType;

import java.time.Instant;
import java.util.UUID;

public record CommunicationResponse(
        UUID id,
        CommunicationType type,
        String content,
        UUID authorId,
        Instant createdAt
) {
}
