package com.nn.ticketapp_api.communication.exception;

import java.util.UUID;

public class AttachmentOwnershipException extends RuntimeException {
    public AttachmentOwnershipException(UUID attachmentId, UUID userId) {
        super(String.format("User %s does not have permission to modify attachment %s", userId, attachmentId));
    }
}
