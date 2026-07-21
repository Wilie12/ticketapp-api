package com.nn.ticketapp_api.communication.controller;

import com.nn.ticketapp_api.communication.api.request.CommunicationCreateRequest;
import com.nn.ticketapp_api.communication.api.response.CommunicationResponse;
import com.nn.ticketapp_api.communication.service.CommunicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
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
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID authorId = extractUserId(jwt);
        log.debug("Received request to add public comment to ticket {} from user: {}", ticketId, authorId);

        return communicationService.addPublicComment(ticketId, authorId, request.content());
    }

    @PostMapping("/work-notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public CommunicationResponse addWorkNote(
            @PathVariable("id") UUID ticketId,
            @Valid @RequestBody CommunicationCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID authorId = extractUserId(jwt);
        log.debug("Received request to add work note to ticket: {} from user: {}", ticketId, authorId);

        return communicationService.addWorkNote(ticketId, authorId, request.content());
    }

    private UUID extractUserId(Jwt jwt) {
        return Optional.ofNullable(jwt.getSubject())
                .map(UUID::fromString)
                .orElseThrow(() -> {
                    log.error("Security violation: JWT token is missing the subject (sub) claim");
                    return new AccessDeniedException("Invalid token: missing subject claim");
                });
    }
}
