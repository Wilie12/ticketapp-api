package com.nn.ticketapp_api.ticket.exception;

import java.util.UUID;

public class TicketOwnershipException extends RuntimeException {
    public TicketOwnershipException(UUID ticketId, UUID userId) {
        super(String.format("User %s is not the owner of ticket %s", userId, ticketId));
    }
}
