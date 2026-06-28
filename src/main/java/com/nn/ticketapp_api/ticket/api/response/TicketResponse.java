package com.nn.ticketapp_api.ticket.api.response;

import com.nn.ticketapp_api.ticket.domain.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String ticketNumber,
        String title,
        TicketStatus status,
        Instant createdAt,
        Instant slaDeadline
) {
}
