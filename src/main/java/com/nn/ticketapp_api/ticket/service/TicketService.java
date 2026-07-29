package com.nn.ticketapp_api.ticket.service;

import com.nn.ticketapp_api.ticket.api.mapper.TicketMapper;
import com.nn.ticketapp_api.ticket.api.request.TicketCreateRequest;
import com.nn.ticketapp_api.ticket.api.request.TicketPatchRequest;
import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import com.nn.ticketapp_api.ticket.exception.TicketClosedException;
import com.nn.ticketapp_api.ticket.exception.TicketNotFoundException;
import com.nn.ticketapp_api.ticket.exception.TicketOwnershipException;
import com.nn.ticketapp_api.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TicketResponse createTicket(TicketCreateRequest ticketCreateRequest, UUID creatorId) {
        log.debug("Creating ticket for user: {}", creatorId);

        long sequenceValue = ticketRepository.getNextTicketNumberSequence();
        String ticketNumber = String.format("INC%07d", sequenceValue);

        Ticket ticket = Ticket.createNew(
                ticketNumber,
                ticketCreateRequest.title(),
                ticketCreateRequest.description(),
                ticketCreateRequest.priority(),
                creatorId,
                ticketCreateRequest.targetTeamId()
        );

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Successfully created ticket: {} with ID: {}", ticketNumber, savedTicket.getId());

        return ticketMapper.toResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getUserTickets(UUID creatorId) {
        log.debug("Retrieving all tickets for user: {}", creatorId);

        return ticketRepository.findAllByCreatorIdOrderByCreatedAtDesc(creatorId)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Ticket getValidatedTicket(UUID ticketId, UUID requesterId) {
        log.debug("Retrieving and validating access for ticket ID: {}", ticketId);

        Ticket ticket = getTicketOrThrow(ticketId);

        if (!ticket.isOwnedBy(requesterId)) {
            log.warn("Security violation: User {} attempted to access ticket {} owned by user {}",
                    requesterId, ticketId, ticket.getCreatorId());
            throw new TicketOwnershipException(ticketId, requesterId);
        }

        return ticket;
    }

    @Transactional
    public TicketResponse assignTicket(UUID ticketId, UUID agentId) {
        log.debug("Assigning ticket {} to agent {}", ticketId, agentId);

        Ticket ticket = getTicketOrThrow(ticketId);
        ticket.assignToAgent(agentId);

        log.info("Ticket {} successfully assigned to agent {}", ticket.getTicketNumber(), agentId);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse resolveTicket(UUID ticketId, UUID agentId, String resolutionNote) {
        log.debug("Resolving ticket {} by agent {}", ticketId, agentId);

        Ticket ticket = getTicketOrThrow(ticketId);
        ticket.resolve();

        eventPublisher.publishEvent(new TicketResolvedEvent(ticketId, agentId, resolutionNote));

        log.info("Ticket {} successfully resolved", ticket.getTicketNumber());
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse closeTicket(UUID ticketId, UUID requesterId) {
        log.debug("Closing ticket {} by user {}", ticketId, requesterId);

        Ticket ticket = getTicketOrThrow(ticketId);

        if (!ticket.isOwnedBy(requesterId)) {
            log.warn(
                    "Security violation: User {} attempted to close ticket {} without ownership",
                    requesterId,
                    ticketId
            );
            throw new TicketOwnershipException(ticketId, requesterId);
        }

        ticket.close();

        log.info("Ticket {} successfully closed", ticket.getTicketNumber());
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse reopenTicket(UUID ticketId, UUID requesterId) {
        log.debug("Reopening ticket {} by user {}", ticketId, requesterId);

        Ticket ticket = getTicketOrThrow(ticketId);

        if (!ticket.isOwnedBy(requesterId)) {
            log.warn(
                    "Security violation: User {} attempted to reopen ticket {} without ownership",
                    requesterId,
                    ticketId
            );
            throw new TicketOwnershipException(ticketId, requesterId);
        }

        ticket.reopen();

        log.info("Ticket {} successfully reopened", ticket.getTicketNumber());
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateTicketDetails(UUID ticketId, TicketPatchRequest ticketPatchRequest, UUID agentId) {
        log.debug("Updating details for ticket {} by agent {}", ticketId, agentId);

        Ticket ticket = getTicketOrThrow(ticketId);

        ticket.updateDetails(
                ticketPatchRequest.title(),
                ticketPatchRequest.priority(),
                ticketPatchRequest.targetTeamId()
        );

        log.info("Details for ticket {} updated successfully", ticket.getTicketNumber());
        return ticketMapper.toResponse(ticket);
    }

    public void ensureTicketIsActive(UUID ticketId) {
        Ticket ticket = getTicketOrThrow(ticketId);

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            log.warn("Attempt to modify CLOSED ticket: {}", ticket.getTicketNumber());
            throw new TicketClosedException(
                    String.format("Cannot modify or add communication to a CLOSED ticket: %s", ticket.getTicketNumber())
            );
        }
    }

    private Ticket getTicketOrThrow(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }
}