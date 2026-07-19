package com.nn.ticketapp_api.communication.service;

import com.nn.ticketapp_api.communication.domain.Communication;
import com.nn.ticketapp_api.communication.domain.CommunicationType;
import com.nn.ticketapp_api.communication.repository.CommunicationRepository;
import com.nn.ticketapp_api.ticket.exception.TicketClosedException;
import com.nn.ticketapp_api.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class CommunicationServiceTest {

    @Mock
    private CommunicationRepository communicationRepository;
    @Mock
    private TicketService ticketService;
    @InjectMocks
    private CommunicationService communicationService;

    @Test
    @DisplayName("Should successfully add a public comment to an active ticket")
    void shouldAddPublicComment() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        String content = "Test comment";

        Communication mockComment = Communication.createComment(ticketId, authorId, content);

        given(communicationRepository.save(any(Communication.class))).willReturn(mockComment);

        // when
        Communication result = communicationService.addPublicComment(ticketId, authorId, content);

        // then
        assertThat(result.getType()).isEqualTo(CommunicationType.PUBLIC_COMMENT);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getTicketId()).isEqualTo(ticketId);

        then(ticketService).should().ensureTicketIsActive(ticketId);
        then(communicationRepository).should().save(any(Communication.class));
    }

    @Test
    @DisplayName("Should successfully add a work note to an active ticket")
    void shouldAddWorkNote() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        String content = "Test work note";

        Communication mockWorkNote = Communication.createWorkNote(ticketId, authorId, content);

        given(communicationRepository.save(any(Communication.class))).willReturn(mockWorkNote);

        // when
        Communication result = communicationService.addWorkNote(ticketId, authorId, content);

        // then
        assertThat(result.getType()).isEqualTo(CommunicationType.WORK_NOTE);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getTicketId()).isEqualTo(ticketId);

        then(ticketService).should().ensureTicketIsActive(ticketId);
        then(communicationRepository).should().save(any(Communication.class));
    }

    @Test
    @DisplayName("Should throw an exception when trying to add communication to a closed ticket")
    void shouldRejectCommunicationForClosedTicket() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        String content = "Test comment";

        willThrow(new TicketClosedException("Cannot modify or add communication to a CLOSED ticket"))
                .given(ticketService).ensureTicketIsActive(ticketId);

        // when
        Throwable thrown = catchThrowable(() -> communicationService.addPublicComment(ticketId, authorId, content));

        // then
        assertThat(thrown)
                .isInstanceOf(TicketClosedException.class)
                .hasMessage("Cannot modify or add communication to a CLOSED ticket");
    }
}
