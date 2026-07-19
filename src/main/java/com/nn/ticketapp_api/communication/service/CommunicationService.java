package com.nn.ticketapp_api.communication.service;

import com.nn.ticketapp_api.communication.domain.Communication;
import com.nn.ticketapp_api.communication.repository.CommunicationRepository;
import com.nn.ticketapp_api.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunicationService {

    private final CommunicationRepository communicationRepository;
    private final TicketService ticketService;

    @Transactional
    public Communication addPublicComment(UUID ticketId, UUID authorId, String content) {
        ticketService.ensureTicketIsActive(ticketId);

        Communication comment = Communication.createComment(ticketId, authorId, content);
        return communicationRepository.save(comment);
    }

    @Transactional
    public Communication addWorkNote(UUID ticketId, UUID authorId, String content) {
        ticketService.ensureTicketIsActive(ticketId);

        Communication workNote = Communication.createWorkNote(ticketId, authorId, content);
        return communicationRepository.save(workNote);
    }
}
