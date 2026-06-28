package com.nn.ticketapp_api.ticket.service;

import com.nn.ticketapp_api.ticket.api.mapper.TicketMapper;
import com.nn.ticketapp_api.ticket.api.request.TicketCreateRequest;
import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.exception.TicketNotFoundException;
import com.nn.ticketapp_api.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public TicketDetailsResponse getTicketDetails(UUID ticketId) {
        log.debug("Retrieving ticket details for ticket ID: {}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        return ticketMapper.toDetailsResponse(ticket);
    }
}
