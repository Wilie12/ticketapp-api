package com.nn.ticketapp_api.communication.api.response;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        String filename,
        String fileUrl,
        Instant uploadedAt
) {
}
