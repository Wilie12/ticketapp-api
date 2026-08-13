package com.nn.ticketapp_api.ticket.domain.event;

import java.util.UUID;

public record TicketCreatedEvent(
        UUID ticketId,
        UUID teamId
) {
}
