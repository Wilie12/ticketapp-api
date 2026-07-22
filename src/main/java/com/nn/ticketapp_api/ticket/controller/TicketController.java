package com.nn.ticketapp_api.ticket.controller;

import com.nn.ticketapp_api.shared.security.annotation.CurrentUserId;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
            @CurrentUserId UUID creatorId
    ) {
        log.debug("Received request to create ticket from user: {}", creatorId);

        return ticketService.createTicket(ticketCreateRequest, creatorId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<TicketResponse> getTickets(@CurrentUserId UUID creatorId) {
        log.debug("Received request to get all tickets for user: {}", creatorId);

        return ticketService.getUserTickets(creatorId);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public TicketDetailsResponse getTicket(
            @PathVariable(name = "id") UUID ticketId,
            @CurrentUserId UUID requesterId
    ) {
        log.debug("Received request from user: {} to get ticket with ID: {}", requesterId, ticketId);

        return ticketService.getTicketDetails(ticketId, requesterId);
    }

    @PostMapping("/{id}/assign")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketResponse assignTicket(
            @PathVariable(name = "id") UUID ticketId,
            @CurrentUserId UUID agentId
    ) {
        log.debug("Received request to assign ticket {} to agent: {}", ticketId, agentId);

        return ticketService.assignTicket(ticketId, agentId);
    }

    @PostMapping("/{id}/resolve")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketResponse resolveTicket(
            @PathVariable(name = "id") UUID ticketId,
            @RequestBody @Valid ResolutionRequest resolutionRequest,
            @CurrentUserId UUID agentId
    ) {
        log.debug("Received request to resolve ticket {} by agent: {}", ticketId, agentId);

        return ticketService.resolveTicket(ticketId, agentId, resolutionRequest.resolutionNote());
    }

    @PostMapping("/{id}/close")
    @ResponseStatus(HttpStatus.OK)
    public TicketResponse closeTicket(
            @PathVariable(name = "id") UUID ticketId,
            @CurrentUserId UUID requesterId
    ) {
        log.debug("Received request to close ticket {} by user: {}", ticketId, requesterId);

        return ticketService.closeTicket(ticketId, requesterId);
    }

    @PostMapping("/{id}/reopen")
    @ResponseStatus(HttpStatus.OK)
    public TicketResponse reopenTicket(
            @PathVariable(name = "id") UUID ticketId,
            @CurrentUserId UUID requesterId
    ) {
        log.debug("Received request to reopen ticket {} by user: {}", ticketId, requesterId);

        return ticketService.reopenTicket(ticketId, requesterId);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketResponse updateTicketDetails(
            @PathVariable(name = "id") UUID ticketId,
            @RequestBody @Valid TicketPatchRequest ticketPatchRequest,
            @CurrentUserId UUID agentId
    ) {
        log.debug("Received request to update details for ticket {} by agent: {}", ticketId, agentId);

        return ticketService.updateTicketDetails(ticketId, ticketPatchRequest, agentId);
    }
}
