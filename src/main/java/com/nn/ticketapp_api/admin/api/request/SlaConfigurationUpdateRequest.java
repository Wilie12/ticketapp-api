package com.nn.ticketapp_api.admin.api.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SlaConfigurationUpdateRequest(
        @NotNull(message = "Resolution hours must not be null")
        @Min(value = 1, message = "Resolution hours must be at least 1 hour")
        Integer resolutionHours
) {
}
