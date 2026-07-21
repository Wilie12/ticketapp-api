package com.nn.ticketapp_api.communication.listener;

import com.nn.ticketapp_api.communication.domain.Communication;
import com.nn.ticketapp_api.communication.repository.CommunicationRepository;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventListener {

    private final CommunicationRepository communicationRepository;

    @EventListener
    public void handleTicketResolvedEvent(TicketResolvedEvent ticketResolvedEvent) {
        log.debug("Received TicketResolvedEvent for ticket ID: {}", ticketResolvedEvent.ticketId());

        String formattedContent = String.format("RESOLUTION NOTE: %s", ticketResolvedEvent.resolutionNote());

        Communication resolutionNote = Communication.createSystemEvent(
                ticketResolvedEvent.ticketId(),
                ticketResolvedEvent.agentId(),
                formattedContent
        );

        communicationRepository.save(resolutionNote);
        log.info("Successfully persisted resolution note for ticket ID: {}", ticketResolvedEvent.ticketId());
    }
}
