package com.nn.ticketapp_api.ticket.service;

import com.nn.ticketapp_api.admin.service.SlaConfigurationService;
import com.nn.ticketapp_api.shared.api.response.PageResponse;
import com.nn.ticketapp_api.ticket.api.mapper.TicketMapper;
import com.nn.ticketapp_api.ticket.api.request.TicketCreateRequest;
import com.nn.ticketapp_api.ticket.api.request.TicketPatchRequest;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import com.nn.ticketapp_api.ticket.domain.policy.SlaPolicy;
import com.nn.ticketapp_api.ticket.exception.TicketClosedException;
import com.nn.ticketapp_api.ticket.exception.TicketNotFoundException;
import com.nn.ticketapp_api.ticket.exception.TicketOwnershipException;
import com.nn.ticketapp_api.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private static final int MAX_ACTIVE_TICKETS = 5;

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SlaPolicy slaPolicy;
    private final SlaConfigurationService slaConfigurationService;
    private final Clock clock;

    @Transactional
    public TicketResponse createTicket(TicketCreateRequest ticketCreateRequest, UUID creatorId) {
        log.debug("Creating ticket for user: {}", creatorId);

        long sequenceValue = ticketRepository.getNextTicketNumberSequence();
        String ticketNumber = String.format("INC%07d", sequenceValue);

        Instant creationTime = Instant.now(clock);

        Integer resolutionHours = slaConfigurationService.getResolutionHours(ticketCreateRequest.priority());
        Instant slaDeadline = slaPolicy.calculateDeadline(creationTime, resolutionHours);

        Ticket ticket = Ticket.createNew(
                ticketNumber,
                ticketCreateRequest.title(),
                ticketCreateRequest.description(),
                ticketCreateRequest.priority(),
                creatorId,
                ticketCreateRequest.targetTeamId(),
                slaDeadline
        );

        Ticket savedTicket = ticketRepository.save(ticket);

        eventPublisher.publishEvent(new TicketCreatedEvent(savedTicket.getId(), savedTicket.getAssignedTeamId()));
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
        ticket.resolve(Instant.now(clock));

        eventPublisher.publishEvent(new TicketResolvedEvent(
                        ticketId,
                        agentId,
                        ticket.getAssignedTeamId(),
                        resolutionNote
                )
        );

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

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getUnassignedQueue(UUID teamId, Pageable pageable) {
        log.debug("Retrieving unassigned ticket queue for team: {}", teamId);

        Page<Ticket> ticketPage = ticketRepository
                .findByStatusAndAssignedTeamIdAndAssignedAgentIdIsNull(TicketStatus.NEW, teamId, pageable);

        return PageResponse.of(ticketPage.map(ticketMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getAgentTickets(UUID agentId, Pageable pageable) {
        log.debug("Retrieving assigned tickets for agent: {}", agentId);

        List<TicketStatus> activeStatuses = List.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);

        Page<Ticket> ticketPage = ticketRepository
                .findAllByAssignedAgentIdAndStatusIn(agentId, activeStatuses, pageable);

        return PageResponse.of(ticketPage.map(ticketMapper::toResponse));
    }

    @Transactional
    public void evaluateAndFillAgentCapacity(UUID agentId, UUID teamId) {
        log.debug("Evaluating capacity for agent: {} in team: {}", agentId, teamId);

        long currentActiveTickets = ticketRepository.countByAssignedAgentIdAndStatus(agentId, TicketStatus.IN_PROGRESS);
        int availableSlots = MAX_ACTIVE_TICKETS - (int) currentActiveTickets;

        if (availableSlots <= 0) {
            log.info("Agent {} is at full capacity ({}). Skipping queue evaluation.", agentId, MAX_ACTIVE_TICKETS);
            return;
        }

        List<Ticket> ticketsToAssign = ticketRepository.findAndLockNextTicketsInQueue(
                TicketStatus.NEW.name(),
                teamId,
                availableSlots
        );

        for (Ticket ticket : ticketsToAssign) {
            ticket.assignToAgent(agentId);
            log.info("Auto-assigned ticket {} from queue to agent {}", ticket.getTicketNumber(), agentId);
        }
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