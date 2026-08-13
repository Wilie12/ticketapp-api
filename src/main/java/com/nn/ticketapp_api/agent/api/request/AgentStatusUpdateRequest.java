package com.nn.ticketapp_api.agent.api.request;

import com.nn.ticketapp_api.agent.domain.AgentStatus;
import jakarta.validation.constraints.NotNull;

public record AgentStatusUpdateRequest(
        @NotNull(message = "Status cannot be null")
        AgentStatus status
) {
}
