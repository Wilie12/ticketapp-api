package com.nn.ticketapp_api.communication.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunicationCreateRequest(
        @NotBlank(message = "Content cannot be blank")
        @Size(min = 2, message = "Content must be at least 2 characters long")
        String content
) {
}
