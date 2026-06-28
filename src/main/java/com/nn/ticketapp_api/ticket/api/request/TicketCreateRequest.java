package com.nn.ticketapp_api.ticket.api.request;

import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TicketCreateRequest(
        @NotBlank(message = "Title cannot be blank")
        @Size(min = 5, message = "Title must be at least 5 characters long")
        String title,
        @NotBlank(message = "Description cannot be blank")
        String description,
        @NotNull(message = "Priority is required")
        TicketPriority priority,
        @NotNull(message = "Target team ID is required")
        UUID targetTeamId
) {
}
