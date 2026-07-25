package com.nn.ticketapp_api.communication.exception;

import java.util.UUID;

public class AttachmentNotFoundException extends RuntimeException {
    public AttachmentNotFoundException(UUID attachmentId) {
        super(String.format("Attachment with ID '%s' not found.", attachmentId));
    }
}
