package com.nn.ticketapp_api.agent.exception;

import java.util.UUID;

public class AgentProfileAlreadyExistsException extends RuntimeException {
    public AgentProfileAlreadyExistsException(UUID agentId) {
        super(String.format("Agent profile with ID %s already exists", agentId));
    }
}
