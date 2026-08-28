package com.nn.ticketapp_api.team.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
        @NotBlank(message = "Team name cannot be blank")
        @Size(min = 3, max = 50, message = "Team name must be between 3 and 50 characters")
        String name,
        @Size(max = 255, message = "Description is too long")
        String description
) {
}
