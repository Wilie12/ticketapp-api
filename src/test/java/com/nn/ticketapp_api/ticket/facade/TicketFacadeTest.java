package com.nn.ticketapp_api.ticket.facade;

import com.nn.ticketapp_api.communication.api.response.AttachmentResponse;
import com.nn.ticketapp_api.communication.api.response.CommunicationResponse;
import com.nn.ticketapp_api.communication.domain.CommunicationType;
import com.nn.ticketapp_api.communication.service.AttachmentService;
import com.nn.ticketapp_api.communication.service.CommunicationService;
import com.nn.ticketapp_api.ticket.api.mapper.TicketMapper;
import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.exception.TicketOwnershipException;
import com.nn.ticketapp_api.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class TicketFacadeTest {

    @Mock
    private TicketService ticketService;
    @Mock
    private CommunicationService communicationService;
    @Mock
    private AttachmentService attachmentService;
    @Mock
    private TicketMapper ticketMapper;
    @InjectMocks
    private TicketFacade ticketFacade;

    @Test
    @DisplayName("Should successfully orchestrate data aggregation for ticket details")
    void shouldAggregateTicketDetails() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        Ticket mockTicket = Ticket.createNew(
                "INC0000001",
                "Title",
                "Desc",
                TicketPriority.LOW,
                requesterId,
                UUID.randomUUID()
        );

        List<CommunicationResponse> comments = List.of(mock(CommunicationResponse.class));
        List<CommunicationResponse> workNotes = List.of(mock(CommunicationResponse.class));
        List<CommunicationResponse> systemEvents = List.of(mock(CommunicationResponse.class));
        List<AttachmentResponse> attachments = List.of(mock(AttachmentResponse.class));

        TicketDetailsResponse expectedResponse = new TicketDetailsResponse(
                ticketId,
                "INC0000001",
                "Title",
                "Desc",
                TicketStatus.NEW,
                null,
                null,
                Instant.now(),
                null,
                comments,
                workNotes,
                systemEvents,
                attachments
        );

        given(ticketService.getValidatedTicket(ticketId, requesterId)).willReturn(mockTicket);
        given(communicationService.getCommunicationsByType(ticketId, CommunicationType.PUBLIC_COMMENT))
                .willReturn(comments);
        given(communicationService.getCommunicationsByType(ticketId, CommunicationType.WORK_NOTE))
                .willReturn(workNotes);
        given(communicationService.getCommunicationsByType(ticketId, CommunicationType.SYSTEM_EVENT))
                .willReturn(systemEvents);
        given(attachmentService.getTicketAttachments(ticketId)).willReturn(attachments);
        given(ticketMapper.toDetailsResponse(mockTicket, comments, workNotes, systemEvents, attachments))
                .willReturn(expectedResponse);

        // when
        TicketDetailsResponse result = ticketFacade.getTicketDetails(ticketId, requesterId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.ticketNumber()).isEqualTo("INC0000001");

        then(ticketService).should().getValidatedTicket(ticketId, requesterId);
        then(communicationService).should().getCommunicationsByType(ticketId, CommunicationType.PUBLIC_COMMENT);
        then(communicationService).should().getCommunicationsByType(ticketId, CommunicationType.WORK_NOTE);
        then(communicationService).should().getCommunicationsByType(ticketId, CommunicationType.SYSTEM_EVENT);
        then(attachmentService).should().getTicketAttachments(ticketId);
    }

    @Test
    @DisplayName("Should fail fast and not fetch communication if ticket validation fails")
    void shouldFailFastOnValidationFailure() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID fakeRequesterId = UUID.randomUUID();

        given(ticketService.getValidatedTicket(ticketId, fakeRequesterId))
                .willThrow(new TicketOwnershipException(ticketId, fakeRequesterId));

        // when
        Throwable thrown = catchThrowable(() -> ticketFacade.getTicketDetails(ticketId, fakeRequesterId));

        // then
        assertThat(thrown)
                .isInstanceOf(TicketOwnershipException.class)
                .hasMessageContaining("is not the owner of ticket");

        then(ticketService).should().getValidatedTicket(ticketId, fakeRequesterId);
        then(communicationService).shouldHaveNoInteractions();
        then(attachmentService).shouldHaveNoInteractions();
        then(ticketMapper).shouldHaveNoInteractions();
    }
}
