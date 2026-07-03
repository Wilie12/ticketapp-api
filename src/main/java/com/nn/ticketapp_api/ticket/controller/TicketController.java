package com.nn.ticketapp_api.ticket.controller;

import com.nn.ticketapp_api.ticket.api.request.TicketCreateRequest;
import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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

    private UUID extractUserId(Jwt jwt) {
        return Optional.ofNullable(jwt.getSubject())
                .map(UUID::fromString)
                .orElseThrow(() -> {
                    log.error("Security violation: JWT token is missing the subject (sub) claim");
                    return new AccessDeniedException("Invalid token: missing subject claim");
                });
    }
}
