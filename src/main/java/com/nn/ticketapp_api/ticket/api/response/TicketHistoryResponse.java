package com.nn.ticketapp_api.ticket.api.response;

import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketHistoryResponse(
        UUID ticketId,
        String ticketNumber,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        UUID assignedAgentId,
        UUID assignedTeamId,
        String revisionType,
        UUID authorId,
        Instant revisionTimestamp
) {
}
