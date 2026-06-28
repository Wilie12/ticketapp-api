package com.nn.ticketapp_api.ticket.exception;

import java.util.UUID;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(UUID ticketId) {
        super(String.format("Ticket with ID %s not found", ticketId));
    }
}
