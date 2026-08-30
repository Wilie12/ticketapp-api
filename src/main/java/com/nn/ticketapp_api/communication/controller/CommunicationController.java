package com.nn.ticketapp_api.communication.controller;

import com.nn.ticketapp_api.communication.api.request.CommunicationCreateRequest;
import com.nn.ticketapp_api.communication.api.response.CommunicationResponse;
import com.nn.ticketapp_api.communication.service.CommunicationService;
import com.nn.ticketapp_api.shared.security.annotation.CurrentRequester;
import com.nn.ticketapp_api.shared.security.domain.RequesterContext;
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
@PreAuthorize("isAuthenticated()")
public class CommunicationController {

    private final CommunicationService communicationService;

    @PostMapping("/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunicationResponse addPublicComment(
            @PathVariable("id") UUID ticketId,
            @Valid @RequestBody CommunicationCreateRequest request,
            @CurrentRequester RequesterContext requesterContext
    ) {
        log.debug(
                "Received request to add public comment to ticket {} from user: {}",
                ticketId,
                requesterContext.userId()
        );

        return communicationService.addPublicComment(ticketId, requesterContext.userId(), request.content());
    }

    @PostMapping("/work-notes")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunicationResponse addWorkNote(
            @PathVariable("id") UUID ticketId,
            @Valid @RequestBody CommunicationCreateRequest request,
            @CurrentRequester RequesterContext requesterContext
    ) {
        requesterContext.requireInternal();

        log.debug("Received request to add work note to ticket: {} from user: {}", ticketId, requesterContext.userId());

        return communicationService.addWorkNote(ticketId, requesterContext.userId(), request.content());
    }
}
