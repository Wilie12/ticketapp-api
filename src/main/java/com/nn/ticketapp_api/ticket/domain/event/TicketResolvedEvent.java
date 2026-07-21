package com.nn.ticketapp_api.ticket.domain.event;

import java.util.UUID;

public record TicketResolvedEvent(
        UUID ticketId,
        UUID agentId,
        String resolutionNote
) {
}
