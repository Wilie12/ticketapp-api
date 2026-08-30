package com.nn.ticketapp_api.team.exception;

import java.util.UUID;

public class TeamNotFoundException extends RuntimeException {
    public TeamNotFoundException(UUID teamId) {
        super(String.format("Team with ID %s not found", teamId));
    }
}
