package com.nn.ticketapp_api.ticket.facade;

import com.nn.ticketapp_api.communication.api.response.AttachmentResponse;
import com.nn.ticketapp_api.communication.api.response.CommunicationResponse;
import com.nn.ticketapp_api.communication.domain.CommunicationType;
import com.nn.ticketapp_api.communication.service.AttachmentService;
import com.nn.ticketapp_api.communication.service.CommunicationService;
import com.nn.ticketapp_api.shared.security.domain.RequesterContext;
import com.nn.ticketapp_api.ticket.api.mapper.TicketMapper;
import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketFacade {

    private final TicketService ticketService;
    private final CommunicationService communicationService;
    private final AttachmentService attachmentService;
    private final TicketMapper ticketMapper;

    @Transactional(readOnly = true)
    public TicketDetailsResponse getTicketDetails(UUID ticketId, RequesterContext requesterContext) {
        log.debug("Orchestrating ticket details aggregation for ticket ID: {}", ticketId);

        Ticket ticket = ticketService.getValidatedTicket(ticketId, requesterContext.userId());

        List<CommunicationResponse> comments = communicationService
                .getCommunicationsByType(ticketId, CommunicationType.PUBLIC_COMMENT);

        List<CommunicationResponse> workNotes = Collections.emptyList();
        List<CommunicationResponse> systemEvents = Collections.emptyList();

        if (requesterContext.isInternal()) {
            workNotes = communicationService.getCommunicationsByType(ticketId, CommunicationType.WORK_NOTE);
            systemEvents = communicationService.getCommunicationsByType(ticketId, CommunicationType.SYSTEM_EVENT);
        }

        List<AttachmentResponse> attachments = attachmentService.getTicketAttachments(ticketId);

        log.debug("Successfully aggregated ticket details for ticket ID: {}", ticketId);

        return ticketMapper.toDetailsResponse(ticket, comments, workNotes, systemEvents, attachments);
    }
}
