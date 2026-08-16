package com.nn.ticketapp_api.admin.api.response;

import com.nn.ticketapp_api.ticket.domain.TicketPriority;

public record SlaConfigurationResponse(
        TicketPriority priority,
        Integer resolutionHours
) {
}
