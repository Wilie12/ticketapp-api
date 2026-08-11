package com.nn.ticketapp_api.agent.exception;

import java.util.UUID;

public class AgentNotFoundException extends RuntimeException {
    public AgentNotFoundException(UUID agentId) {
        super(String.format("Agent profile with ID: %s not found", agentId));
    }
}
