package com.nn.ticketapp_api.team.api.request;

import jakarta.validation.constraints.Size;

public record TeamUpdateRequest(
        @Size(min = 5, max = 50, message = "Team name must be between 3 and 50 characters")
        String name,
        @Size(max = 255, message = "Description is too long")
        String description
) {
}
