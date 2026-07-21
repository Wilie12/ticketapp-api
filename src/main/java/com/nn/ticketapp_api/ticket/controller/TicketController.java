package com.nn.ticketapp_api.ticket.controller;

import com.nn.ticketapp_api.ticket.api.request.ResolutionRequest;
import com.nn.ticketapp_api.ticket.api.request.TicketCreateRequest;
import com.nn.ticketapp_api.ticket.api.request.TicketPatchRequest;
import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(
            @RequestBody @Valid TicketCreateRequest ticketCreateRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID creatorId = extractUserId(jwt);

        log.debug("Received request to create ticket from user: {}", creatorId);

        return ticketService.createTicket(ticketCreateRequest, creatorId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<TicketResponse> getTickets(@AuthenticationPrincipal Jwt jwt) {
        UUID creatorId = extractUserId(jwt);

        log.debug("Received request to get all tickets for user: {}", creatorId);

        return ticketService.getUserTickets(creatorId);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public TicketDetailsResponse getTicket(
            @PathVariable(name = "id") UUID ticketId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID requesterId = extractUserId(jwt);

        log.debug("Received request from user: {} to get ticket with ID: {}", requesterId, ticketId);

        return ticketService.getTicketDetails(ticketId, requesterId);
    }

    @PostMapping("/{id}/assign")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketResponse assignTicket(
            @PathVariable(name = "id") UUID ticketId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID agentId = extractUserId(jwt);

        log.debug("Received request to assign ticket {} to agent: {}", ticketId, agentId);

        return ticketService.assignTicket(ticketId, agentId);
    }

    @PostMapping("/{id}/resolve")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketResponse resolveTicket(
            @PathVariable(name = "id") UUID ticketId,
            @RequestBody @Valid ResolutionRequest resolutionRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID agentId = extractUserId(jwt);

        log.debug("Received request to resolve ticket {} by agent: {}", ticketId, agentId);

        return ticketService.resolveTicket(ticketId, agentId, resolutionRequest.resolutionNote());
    }

    @PostMapping("/{id}/close")
    @ResponseStatus(HttpStatus.OK)
    public TicketResponse closeTicket(
            @PathVariable(name = "id") UUID ticketId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID requesterId = extractUserId(jwt);

        log.debug("Received request to close ticket {} by user: {}", ticketId, requesterId);

        return ticketService.closeTicket(ticketId, requesterId);
    }

    @PostMapping("/{id}/reopen")
    @ResponseStatus(HttpStatus.OK)
    public TicketResponse reopenTicket(
            @PathVariable(name = "id") UUID ticketId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID requesterId = extractUserId(jwt);

        log.debug("Received request to reopen ticket {} by user: {}", ticketId, requesterId);

        return ticketService.reopenTicket(ticketId, requesterId);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketResponse updateTicketDetails(
            @PathVariable(name = "id") UUID ticketId,
            @RequestBody @Valid TicketPatchRequest ticketPatchRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID agentId = extractUserId(jwt);

        log.debug("Received request to update details for ticket {} by agent: {}", ticketId, agentId);

        return ticketService.updateTicketDetails(ticketId, ticketPatchRequest, agentId);
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
