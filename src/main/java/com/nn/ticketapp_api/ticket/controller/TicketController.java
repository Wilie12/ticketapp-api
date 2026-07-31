package com.nn.ticketapp_api.ticket.controller;

import com.nn.ticketapp_api.shared.security.annotation.CurrentRequester;
import com.nn.ticketapp_api.shared.security.domain.RequesterContext;
import com.nn.ticketapp_api.ticket.api.request.ResolutionRequest;
import com.nn.ticketapp_api.ticket.api.request.TicketCreateRequest;
import com.nn.ticketapp_api.ticket.api.request.TicketPatchRequest;
import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.facade.TicketFacade;
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
    private final TicketFacade ticketFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(
            @RequestBody @Valid TicketCreateRequest ticketCreateRequest,
            @CurrentRequester RequesterContext requesterContext
    ) {
        log.debug("Received request to create ticket from user: {}", requesterContext.userId());

        return ticketService.createTicket(ticketCreateRequest, requesterContext.userId());
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<TicketResponse> getTickets(@CurrentRequester RequesterContext requesterContext) {
        log.debug("Received request to get all tickets for user: {}", requesterContext.userId());

        return ticketService.getUserTickets(requesterContext.userId());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public TicketDetailsResponse getTicket(
            @PathVariable(name = "id") UUID ticketId,
            @CurrentRequester RequesterContext requesterContext
    ) {
        log.debug("Received request from user: {} to get ticket with ID: {}", requesterContext.userId(), ticketId);

        return ticketFacade.getTicketDetails(ticketId, requesterContext);
    }

    @PostMapping("/{id}/assign")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketResponse assignTicket(
            @PathVariable(name = "id") UUID ticketId,
            @CurrentRequester RequesterContext requesterContext
    ) {
        log.debug("Received request to assign ticket {} to agent: {}", ticketId, requesterContext.userId());

        return ticketService.assignTicket(ticketId, requesterContext.userId());
    }

    @PostMapping("/{id}/resolve")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketResponse resolveTicket(
            @PathVariable(name = "id") UUID ticketId,
            @RequestBody @Valid ResolutionRequest resolutionRequest,
            @CurrentRequester RequesterContext requesterContext
    ) {
        log.debug("Received request to resolve ticket {} by agent: {}", ticketId, requesterContext.userId());

        return ticketService.resolveTicket(ticketId, requesterContext.userId(), resolutionRequest.resolutionNote());
    }

    @PostMapping("/{id}/close")
    @ResponseStatus(HttpStatus.OK)
    public TicketResponse closeTicket(
            @PathVariable(name = "id") UUID ticketId,
            @CurrentRequester RequesterContext requesterContext
    ) {
        log.debug("Received request to close ticket {} by user: {}", ticketId, requesterContext.userId());

        return ticketService.closeTicket(ticketId, requesterContext.userId());
    }

    @PostMapping("/{id}/reopen")
    @ResponseStatus(HttpStatus.OK)
    public TicketResponse reopenTicket(
            @PathVariable(name = "id") UUID ticketId,
            @CurrentRequester RequesterContext requesterContext
    ) {
        log.debug("Received request to reopen ticket {} by user: {}", ticketId, requesterContext.userId());

        return ticketService.reopenTicket(ticketId, requesterContext.userId());
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketResponse updateTicketDetails(
            @PathVariable(name = "id") UUID ticketId,
            @RequestBody @Valid TicketPatchRequest ticketPatchRequest,
            @CurrentRequester RequesterContext requesterContext
    ) {
        log.debug("Received request to update details for ticket {} by agent: {}", ticketId, requesterContext.userId());

        return ticketService.updateTicketDetails(ticketId, ticketPatchRequest, requesterContext.userId());
    }
}
