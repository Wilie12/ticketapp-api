package com.nn.ticketapp_api.ticket.service;

import com.nn.ticketapp_api.shared.api.response.PageResponse;
import com.nn.ticketapp_api.ticket.api.mapper.TicketMapper;
import com.nn.ticketapp_api.ticket.api.request.TicketCreateRequest;
import com.nn.ticketapp_api.ticket.api.request.TicketPatchRequest;
import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import com.nn.ticketapp_api.ticket.domain.policy.SlaPolicy;
import com.nn.ticketapp_api.ticket.exception.TicketNotFoundException;
import com.nn.ticketapp_api.ticket.exception.TicketOwnershipException;
import com.nn.ticketapp_api.ticket.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-03T10:00:00Z");

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private SlaPolicy slaPolicy;
    @Spy
    private Clock clock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
    @InjectMocks
    private TicketService ticketService;

    @Test
    @DisplayName("Should create ticket calculating SLA dynamically and return response")
    void shouldCreateTicket() {
        // given
        UUID creatorId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        TicketCreateRequest request = new TicketCreateRequest(
                "Test ticket", "Desc", TicketPriority.LOW, teamId
        );

        Instant expectedSlaDeadline = FIXED_NOW.plusSeconds(7200);

        given(ticketRepository.getNextTicketNumberSequence()).willReturn(1L);
        given(slaPolicy.calculateDeadline(eq(TicketPriority.LOW), eq(FIXED_NOW))).willReturn(expectedSlaDeadline);

        Ticket savedTicket = buildTicket(
                UUID.randomUUID(),
                "INC0000001",
                TicketStatus.NEW,
                creatorId,
                teamId,
                null
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

        // when
        TicketResponse actualResponse = ticketService.createTicket(request, creatorId);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.ticketNumber()).isEqualTo("INC0000001");

        then(ticketRepository).should().getNextTicketNumberSequence();
        then(slaPolicy).should().calculateDeadline(eq(TicketPriority.LOW), eq(FIXED_NOW));
        then(ticketRepository).should().save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should return list of user tickets")
    void shouldReturnUserTickets() {
        // given
        UUID creatorId = UUID.randomUUID();
        Ticket ticket = buildTicket(
                UUID.randomUUID(),
                "INC0000002",
                TicketStatus.NEW,
                creatorId,
                null,
                null
        );
        TicketResponse expectedResponse = new TicketResponse(
                UUID.randomUUID(),
                "INC0000002",
                "Test ticket",
                TicketStatus.NEW,
                Instant.now(),
                null
        );

        given(ticketRepository.findAllByCreatorIdOrderByCreatedAtDesc(creatorId)).willReturn(List.of(ticket));
        given(ticketMapper.toResponse(ticket)).willReturn(expectedResponse);

        // when
        List<TicketResponse> actualResponse = ticketService.getUserTickets(creatorId);

        // then
        assertThat(actualResponse).hasSize(1);
        assertThat(actualResponse.get(0).ticketNumber()).isEqualTo("INC0000002");

        then(ticketRepository).should().findAllByCreatorIdOrderByCreatedAtDesc(creatorId);
        then(ticketMapper).should().toResponse(ticket);
    }

    @Test
    @DisplayName("Should return validated ticket entity when user owns the ticket")
    void shouldReturnValidatedTicket() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Ticket ticket = buildTicket(
                ticketId,
                "INC0000003",
                TicketStatus.NEW,
                creatorId,
                UUID.randomUUID(),
                null
        );

        given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticket));

        // when
        Ticket actualResponse = ticketService.getValidatedTicket(ticketId, creatorId);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getTicketNumber()).isEqualTo("INC0000003");

        then(ticketRepository).should().findById(ticketId);
    }

    @Test
    @DisplayName("Should throw TicketNotFoundException when ticket does not exist during validation")
    void shouldThrowExceptionWhenTicketNotFound() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        given(ticketRepository.findById(ticketId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> ticketService.getValidatedTicket(ticketId, creatorId));

        // then
        assertThat(thrown)
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("Ticket with ID " + ticketId + " not found");
    }

    @Test
    @DisplayName("Should assign ticket to agent and change status to IN_PROGRESS")
    void shouldAssignTicketToAgent() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        Ticket ticket = buildTicket(
                ticketId,
                "INC0000004",
                TicketStatus.NEW,
                null,
                null,
                null
        );

        given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticket));

        TicketResponse expectedResponse = new TicketResponse(
                ticketId,
                "INC0000004",
                "Title for INC0000004",
                TicketStatus.IN_PROGRESS,
                Instant.now(),
                null
        );
        given(ticketMapper.toResponse(ticket)).willReturn(expectedResponse);

        // when
        TicketResponse actualResponse = ticketService.assignTicket(ticketId, agentId);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getAssignedAgentId()).isEqualTo(agentId);

        then(ticketRepository).should().findById(ticketId);
        then(ticketMapper).should().toResponse(ticket);
    }

    @Test
    @DisplayName("Should resolve ticket and change status to RESOLVED")
    void shouldResolveTicket() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        Ticket ticket = buildTicket(
                ticketId,
                "INC0000005",
                TicketStatus.IN_PROGRESS,
                null,
                null,
                agentId
        );

        given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticket));

        // when
        ticketService.resolveTicket(ticketId, agentId, "Resolution details");

        // then
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getResolvedAt()).isEqualTo(FIXED_NOW);

        then(ticketRepository).should().findById(ticketId);
        then(ticketMapper).should().toResponse(ticket);
        then(applicationEventPublisher).should().publishEvent(any(TicketResolvedEvent.class));
    }

    @Test
    @DisplayName("Should successfully close ticket when requester is the owner")
    void shouldCloseTicketWhenRequestedByOwner() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Ticket ticket = buildTicket(
                ticketId,
                "INC0000006",
                TicketStatus.RESOLVED,
                ownerId,
                null,
                null
        );

        given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticket));

        // when
        ticketService.closeTicket(ticketId, ownerId);

        // then
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);

        then(ticketRepository).should().findById(ticketId);
        then(ticketMapper).should().toResponse(ticket);
    }

    @Test
    @DisplayName("Should throw TicketOwnershipException when non-owner tries to close ticket")
    void shouldThrowExceptionWhenNonOwnerClosesTicket() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID maliciousUserId = UUID.randomUUID();

        Ticket ticket = buildTicket(
                ticketId,
                "INC0000007",
                TicketStatus.RESOLVED,
                ownerId,
                null,
                null
        );

        given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticket));

        // when
        Throwable thrown = catchThrowable(() -> ticketService.closeTicket(ticketId, maliciousUserId));

        // when
        assertThat(thrown)
                .isInstanceOf(TicketOwnershipException.class)
                .hasMessageContaining("is not the owner of ticket");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);

        then(ticketRepository).should().findById(ticketId);
    }

    @Test
    @DisplayName("Should successfully reopen ticket when requester is the owner")
    void shouldReopenTicketWhenRequestedByOwner() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Ticket ticket = buildTicket(
                ticketId,
                "INC0000008",
                TicketStatus.RESOLVED,
                ownerId,
                null,
                null
        );

        given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticket));

        // when
        ticketService.reopenTicket(ticketId, ownerId);

        // then
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        then(ticketRepository).should().findById(ticketId);
        then(ticketMapper).should().toResponse(ticket);
    }

    @Test
    @DisplayName("Should throw TicketOwnershipException when non-owner tries to reopen ticket")
    void shouldThrowExceptionWhenNonOwnerReopensTicket() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID maliciousUserId = UUID.randomUUID();

        Ticket ticket = buildTicket(
                ticketId,
                "INC0000009",
                TicketStatus.RESOLVED,
                ownerId,
                null,
                null
        );

        given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticket));

        // when
        Throwable thrown = catchThrowable(() -> ticketService.reopenTicket(ticketId, maliciousUserId));

        // then
        assertThat(thrown)
                .isInstanceOf(TicketOwnershipException.class)
                .hasMessageContaining("is not the owner of ticket");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);

        then(ticketRepository).should().findById(ticketId);
    }

    @Test
    @DisplayName("Should update ticket details")
    void shouldUpdateTicketDetails() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UUID newTeamId = UUID.randomUUID();

        Ticket ticket = buildTicket(
                ticketId,
                "INC0000010",
                TicketStatus.IN_PROGRESS,
                null,
                UUID.randomUUID(),
                agentId
        );

        TicketPatchRequest request = new TicketPatchRequest("New Title", TicketPriority.HIGH, newTeamId);

        given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticket));

        // when
        ticketService.updateTicketDetails(ticketId, request, agentId);

        // then
        assertThat(ticket.getTitle()).isEqualTo("New Title");
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(ticket.getAssignedTeamId()).isEqualTo(newTeamId);

        then(ticketRepository).should().findById(ticketId);
        then(ticketMapper).should().toResponse(ticket);
    }

    @Test
    @DisplayName("Should return paginated unassigned queue for a team")
    void shouldReturnUnassignedQueue() {
        // given
        UUID teamId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Ticket ticket = buildTicket(
                teamId,
                "INC0000011",
                TicketStatus.NEW,
                UUID.randomUUID(),
                teamId,
                null
        );

        Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket), pageable, 1);

        TicketResponse ticketResponse = new TicketResponse(
                ticket.getId(),
                "INC0000011",
                "Title for INC0000011",
                TicketStatus.NEW,
                Instant.now(),
                null
        );

        given(ticketRepository
                .findByStatusAndAssignedTeamIdAndAssignedAgentIdIsNull(TicketStatus.NEW, teamId, pageable))
                .willReturn(ticketPage);
        given(ticketMapper.toResponse(ticket)).willReturn(ticketResponse);

        // when
        PageResponse<TicketResponse> result = ticketService.getUnassignedQueue(teamId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).ticketNumber()).isEqualTo("INC0000011");

        then(ticketRepository).should()
                .findByStatusAndAssignedTeamIdAndAssignedAgentIdIsNull(TicketStatus.NEW, teamId, pageable);
        then(ticketMapper).should().toResponse(ticket);
    }

    @Test
    @DisplayName("Should return paginated assigned tickets for an agent")
    void shouldReturnAgentTickets() {
        // given
        UUID agentId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Ticket ticket = buildTicket(
                UUID.randomUUID(),
                "INC0000012",
                TicketStatus.NEW,
                UUID.randomUUID(),
                UUID.randomUUID(),
                agentId
        );

        Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket), pageable, 1);

        TicketResponse ticketResponse = new TicketResponse(
                ticket.getId(),
                "INC0000012",
                "Title for INC0000012",
                TicketStatus.IN_PROGRESS,
                Instant.now(),
                null
        );

        List<TicketStatus> activeStatuses = List.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);

        given(ticketRepository.findAllByAssignedAgentIdAndStatusIn(agentId, activeStatuses, pageable))
                .willReturn(ticketPage);
        given(ticketMapper.toResponse(ticket)).willReturn(ticketResponse);

        // when
        PageResponse<TicketResponse> result = ticketService.getAgentTickets(agentId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).ticketNumber()).isEqualTo("INC0000012");

        then(ticketRepository).should().findAllByAssignedAgentIdAndStatusIn(agentId, activeStatuses, pageable);
        then(ticketMapper).should().toResponse(ticket);
    }

    private Ticket buildTicket(
            UUID id,
            String ticketNumber,
            TicketStatus ticketStatus,
            UUID creatorId,
            UUID teamId,
            UUID agentId
    ) {
        return Ticket.builder()
                .id(id != null ? id : UUID.randomUUID())
                .ticketNumber(ticketNumber != null ? ticketNumber : "INC0000001")
                .title(ticketNumber != null ? "Title for " + ticketNumber : "Default Title")
                .description("Test description")
                .priority(TicketPriority.LOW)
                .status(ticketStatus != null ? ticketStatus : TicketStatus.NEW)
                .creatorId(creatorId != null ? creatorId : UUID.randomUUID())
                .assignedTeamId(teamId)
                .assignedAgentId(agentId)
                .build();
    }
}
