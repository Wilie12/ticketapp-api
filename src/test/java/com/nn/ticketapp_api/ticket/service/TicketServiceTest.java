package com.nn.ticketapp_api.ticket.service;

import com.nn.ticketapp_api.ticket.api.mapper.TicketMapper;
import com.nn.ticketapp_api.ticket.api.request.TicketCreateRequest;
import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.exception.TicketNotFoundException;
import com.nn.ticketapp_api.ticket.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketMapper ticketMapper;
    @InjectMocks
    private TicketService ticketService;

    @Test
    @DisplayName("Should create ticket and return response")
    void shouldCreateTicket() {
        UUID creatorId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        TicketCreateRequest request = new TicketCreateRequest(
                "Test ticket", "Desc", TicketPriority.LOW, teamId
        );

        given(ticketRepository.getNextTicketNumberSequence()).willReturn(1L);

        Ticket savedTicket = Ticket.createNew(
                "INC0000001", request.title(), request.description(), request.priority(), creatorId, teamId
        );
        given(ticketRepository.save(any(Ticket.class))).willReturn(savedTicket);

        TicketResponse expectedResponse = new TicketResponse(
                UUID.randomUUID(),
                "INC0000001",
                "Test ticket",
                TicketStatus.NEW,
                Instant.now(),
                null
        );
        given(ticketMapper.toResponse(savedTicket)).willReturn(expectedResponse);

        TicketResponse actualResponse = ticketService.createTicket(request, creatorId);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.ticketNumber()).isEqualTo("INC0000001");

        verify(ticketRepository).getNextTicketNumberSequence();
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should return list of user tickets")
    void shouldReturnUserTickets() {
        UUID creatorId = UUID.randomUUID();
        Ticket ticket = Ticket.createNew(
                "INC0000001",
                "Test ticket",
                "Desc",
                TicketPriority.LOW,
                creatorId,
                UUID.randomUUID()
        );
        TicketResponse expectedResponse = new TicketResponse(
                UUID.randomUUID(),
                "INC0000001",
                "Test ticket",
                TicketStatus.NEW,
                Instant.now(),
                null
        );

        given(ticketRepository.findAllByCreatorIdOrderByCreatedAtDesc(creatorId)).willReturn(List.of(ticket));
        given(ticketMapper.toResponse(ticket)).willReturn(expectedResponse);

        List<TicketResponse> actualResponse = ticketService.getUserTickets(creatorId);

        assertThat(actualResponse).hasSize(1);
        assertThat(actualResponse.get(0).ticketNumber()).isEqualTo("INC0000001");

        verify(ticketRepository).findAllByCreatorIdOrderByCreatedAtDesc(creatorId);
        verify(ticketMapper).toResponse(ticket);
    }

    @Test
    @DisplayName("Should return ticket details when ticket exists")
    void shouldReturnTicketDetails() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.createNew(
                "INC0000001",
                "Test ticket",
                "Desc",
                TicketPriority.LOW,
                UUID.randomUUID(),
                UUID.randomUUID()
        );
        TicketDetailsResponse expectedResponse = new TicketDetailsResponse(
                ticketId,
                "INC0000001",
                "Test ticket",
                "Desc",
                TicketStatus.NEW,
                null,
                null,
                Instant.now(),
                null
        );

        given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticket));
        given(ticketMapper.toDetailsResponse(ticket)).willReturn(expectedResponse);

        TicketDetailsResponse actualResponse = ticketService.getTicketDetails(ticketId);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.ticketNumber()).isEqualTo("INC0000001");

        verify(ticketRepository).findById(ticketId);
    }

    @Test
    @DisplayName("Should throw TicketNotFoundException when ticket does not exist")
    void shouldThrowExceptionWhenTicketNotFound() {
        UUID ticketId = UUID.randomUUID();
        given(ticketRepository.findById(ticketId)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> ticketService.getTicketDetails(ticketId));

        assertThat(thrown)
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("Ticket with ID " + ticketId + " not found");
    }
}
