package com.nn.ticketapp_api.ticket.api.request;

import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TicketPatchRequest(
        @Size(min = 5, message = "Title must be at least 5 characters long")
        String title,
        TicketPriority priority,
        UUID targetTeamId
) {
}
