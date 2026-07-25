package com.nn.ticketapp_api.communication.controller;

import com.nn.ticketapp_api.communication.api.response.AttachmentResponse;
import com.nn.ticketapp_api.communication.service.AttachmentService;
import com.nn.ticketapp_api.shared.security.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse uploadAttachment(
            @PathVariable("ticketId") UUID ticketId,
            @RequestParam("file") MultipartFile file,
            @CurrentUserId UUID authorId
    ) {
        log.debug("Received request to upload attachment to ticket {} from user: {}", ticketId, authorId);

        return attachmentService.uploadAttachment(ticketId, authorId, file);
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(
            @PathVariable("ticketId") UUID ticketId,
            @PathVariable("attachmentId") UUID attachmentId,
            @CurrentUserId UUID authorId
    ) {
        log.debug(
                "Received request to delete attachment {} from ticket {} from user: {}",
                attachmentId,
                ticketId,
                authorId
        );

        attachmentService.deleteAttachment(ticketId, attachmentId, authorId);
    }
}
