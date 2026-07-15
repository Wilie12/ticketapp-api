package com.nn.ticketapp_api.ticket.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolutionRequest(
        @NotBlank(message = "Resolution note must not be blank")
        @Size(min = 10, message = "Resolution note must be at least 10 characters long")
        String resolutionNote
) {
}
