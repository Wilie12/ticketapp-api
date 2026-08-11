package com.nn.ticketapp_api.agent.domain.event;

import java.util.UUID;

public record AgentAvailableEvent(
        UUID agentId,
        UUID teamId
) {
}
