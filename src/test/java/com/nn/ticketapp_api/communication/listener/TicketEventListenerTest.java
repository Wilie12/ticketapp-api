package com.nn.ticketapp_api.communication.listener;

import com.nn.ticketapp_api.communication.domain.Communication;
import com.nn.ticketapp_api.communication.domain.CommunicationType;
import com.nn.ticketapp_api.communication.repository.CommunicationRepository;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class TicketEventListenerTest {

    @Mock
    private CommunicationRepository communicationRepository;
    @InjectMocks
    private TicketEventListener ticketEventListener;
    @Captor
    private ArgumentCaptor<Communication> communicationArgumentCaptor;

    @Test
    @DisplayName("Should intercept TicketResolvedEvent and save as a SYSTEM_EVENT communication")
    void shouldHandleTicketResolvedEvent() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        String resolutionNote = "Test resolution note";

        TicketResolvedEvent ticketResolvedEvent = new TicketResolvedEvent(ticketId, agentId, teamId, resolutionNote);

        // when
        ticketEventListener.handleTicketResolvedEvent(ticketResolvedEvent);

        // then
        then(communicationRepository).should().save(communicationArgumentCaptor.capture());
        Communication savedCommunication = communicationArgumentCaptor.getValue();

        assertThat(savedCommunication.getTicketId()).isEqualTo(ticketId);
        assertThat(savedCommunication.getAuthorId()).isEqualTo(agentId);
        assertThat(savedCommunication.getType()).isEqualTo(CommunicationType.SYSTEM_EVENT);
        assertThat(savedCommunication.getContent()).isEqualTo("RESOLUTION NOTE: " + resolutionNote);
    }
}
