package com.nn.ticketapp_api.ticket.exception;

public class TicketClosedException extends RuntimeException {
    public TicketClosedException(String message) {
        super(message);
    }
}
