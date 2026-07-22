package com.nn.ticketapp_api.communication.controller;

import com.nn.ticketapp_api.communication.api.request.CommunicationCreateRequest;
import com.nn.ticketapp_api.communication.api.response.CommunicationResponse;
import com.nn.ticketapp_api.communication.service.CommunicationService;
import com.nn.ticketapp_api.shared.security.annotation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tickets/{id}")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
public class CommunicationController {

    private final CommunicationService communicationService;

    @PostMapping("/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunicationResponse addPublicComment(
            @PathVariable("id") UUID ticketId,
            @Valid @RequestBody CommunicationCreateRequest request,
            @CurrentUserId UUID authorId
    ) {
        log.debug("Received request to add public comment to ticket {} from user: {}", ticketId, authorId);

        return communicationService.addPublicComment(ticketId, authorId, request.content());
    }

    @PostMapping("/work-notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public CommunicationResponse addWorkNote(
            @PathVariable("id") UUID ticketId,
            @Valid @RequestBody CommunicationCreateRequest request,
            @CurrentUserId UUID authorId
    ) {
        log.debug("Received request to add work note to ticket: {} from user: {}", ticketId, authorId);

        return communicationService.addWorkNote(ticketId, authorId, request.content());
    }
}
