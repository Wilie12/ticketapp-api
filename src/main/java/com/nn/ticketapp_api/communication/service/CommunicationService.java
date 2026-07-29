package com.nn.ticketapp_api.communication.service;

import com.nn.ticketapp_api.communication.api.mapper.CommunicationMapper;
import com.nn.ticketapp_api.communication.api.response.CommunicationResponse;
import com.nn.ticketapp_api.communication.domain.Communication;
import com.nn.ticketapp_api.communication.domain.CommunicationType;
import com.nn.ticketapp_api.communication.repository.CommunicationRepository;
import com.nn.ticketapp_api.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationService {

    private final CommunicationRepository communicationRepository;
    private final TicketService ticketService;
    private final CommunicationMapper communicationMapper;

    @Transactional
    public CommunicationResponse addPublicComment(UUID ticketId, UUID authorId, String content) {
        log.debug("Adding public comment to ticket {} by user {}", ticketId, authorId);

        ticketService.ensureTicketIsActive(ticketId);

        Communication comment = Communication.createComment(ticketId, authorId, content);
        Communication savedComment = communicationRepository.save(comment);

        log.info("Successfully added public comment to ticket {} by user {}", ticketId, authorId);
        return communicationMapper.toResponse(savedComment);
    }

    @Transactional
    public CommunicationResponse addWorkNote(UUID ticketId, UUID authorId, String content) {
        log.debug("Adding work note to ticket {} by user {}", ticketId, authorId);

        ticketService.ensureTicketIsActive(ticketId);

        Communication workNote = Communication.createWorkNote(ticketId, authorId, content);
        Communication savedWorkNote = communicationRepository.save(workNote);

        log.info("Successfully added work note to ticket {} by user {}", ticketId, authorId);
        return communicationMapper.toResponse(savedWorkNote);
    }

    @Transactional(readOnly = true)
    public List<CommunicationResponse> getCommunicationsByType(UUID ticketId, CommunicationType type) {
        log.debug("Retrieving communications of type {} for ticket {}", type, ticketId);

        return communicationRepository.findByTicketIdAndTypeOrderByCreatedAtAsc(ticketId, type)
                .stream()
                .map(communicationMapper::toResponse)
                .toList();
    }
}
