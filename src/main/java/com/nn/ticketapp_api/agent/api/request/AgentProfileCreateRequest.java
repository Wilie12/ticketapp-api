package com.nn.ticketapp_api.agent.api.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AgentProfileCreateRequest(
        @NotNull(message = "Agent ID cannot be null")
        UUID agentId,
        @NotNull(message = "Team ID cannot be null")
        UUID teamId
) {
}
