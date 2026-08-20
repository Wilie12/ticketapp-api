package com.nn.ticketapp_api.agent.api.response;

import com.nn.ticketapp_api.agent.domain.AgentStatus;

import java.time.Instant;
import java.util.UUID;

public record AgentProfileResponse(
        UUID agentId,
        UUID teamId,
        AgentStatus status,
        Instant updatedAt
) {
}
