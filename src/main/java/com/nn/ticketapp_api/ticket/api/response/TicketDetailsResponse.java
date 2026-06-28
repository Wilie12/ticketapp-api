package com.nn.ticketapp_api.ticket.api.response;

import com.nn.ticketapp_api.ticket.domain.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketDetailsResponse(
        UUID id,
        String ticketNumber,
        String title,
        String description,
        TicketStatus status,
        UUID assignedAgentId,
        UUID assignedTeamId,
        Instant createdAt,
        Instant slaDeadline
) {
}
