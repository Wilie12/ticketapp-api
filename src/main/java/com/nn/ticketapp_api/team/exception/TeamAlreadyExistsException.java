package com.nn.ticketapp_api.team.exception;

import java.util.UUID;

public class TeamAlreadyExistsException extends RuntimeException {
    public TeamAlreadyExistsException(String name) {
        super(String.format("Team with name %s already exists", name));
    }
}
