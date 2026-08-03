package com.nn.ticketapp_api.ticket.domain;

import com.nn.ticketapp_api.ticket.exception.InvalidStatusTransitionException;
import com.nn.ticketapp_api.ticket.exception.TicketClosedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

public class TicketTest {
    private final UUID creatorId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID agentId = UUID.randomUUID();
    private static final Instant FIXED_NOW = Instant.parse("2026-08-03T10:00:00Z");

    @Test
    @DisplayName("Should create a NEW ticket with correct initial state")
    void shouldCreateNewTicket() {
        // given
        Instant expectedSlaDeadline = FIXED_NOW.plusSeconds(7200);

        // when
        Ticket ticket = Ticket.createNew(
                "INC0000001",
                "Test ticket",
                "Desc",
                TicketPriority.LOW,
                creatorId,
                teamId,
                expectedSlaDeadline
        );

        // then
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(ticket.getTicketNumber()).isEqualTo("INC0000001");
        assertThat(ticket.getCreatorId()).isEqualTo(creatorId);
        assertThat(ticket.getAssignedTeamId()).isEqualTo(teamId);
        assertThat(ticket.getAssignedAgentId()).isNull();
        assertThat(ticket.getSlaDeadline()).isEqualTo(expectedSlaDeadline);
    }

    @Test
    @DisplayName("Should successfully assign a NEW ticket to an agent")
    void shouldAssignToAgent() {
        // given
        Ticket ticket = createBaseTicket(TicketStatus.NEW);

        // when
        ticket.assignToAgent(agentId);

        // then
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getAssignedAgentId()).isEqualTo(agentId);
    }

    @Test
    @DisplayName("Should throw exception when trying to assign ticket that is not NEW")
    void shouldFailToAssignIfTicketIsNotNew() {
        // given
        Ticket ticket = createBaseTicket(TicketStatus.IN_PROGRESS);

        // when
        Throwable thrown = catchThrowable(() -> ticket.assignToAgent(agentId));

        // then
        assertThat(thrown)
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Only NEW tickets can be assigned");
    }

    @Test
    @DisplayName("Should successfully resolve an IN_PROGRESS ticket")
    void shouldResolveInProgressTicket() {
        // given
        Ticket ticket = createBaseTicket(TicketStatus.IN_PROGRESS);

        // when
        ticket.resolve(FIXED_NOW);

        // then
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getResolvedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    @DisplayName("Should successfully close a RESOLVED ticket")
    void shouldCloseTicket() {
        // given
        Ticket ticket = createBaseTicket(TicketStatus.RESOLVED);

        // when
        ticket.close();

        // then
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("Should successfully reopen a RESOLVED ticket and clear resolvedAt")
    void shouldReopenTicket() {
        // given
        Ticket ticket = createBaseTicket(TicketStatus.IN_PROGRESS);
        ticket.resolve(FIXED_NOW);

        // when
        ticket.reopen();

        // then
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("Should block reopening of a CLOSED ticket")
    void shouldFailToReopenClosedTicket() {
        // given
        Ticket ticket = createBaseTicket(TicketStatus.CLOSED);

        // when
        Throwable thrown = catchThrowable(ticket::reopen);

        // then
        assertThat(thrown)
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Only RESOLVED tickets can be reopened");
    }

    @Test
    @DisplayName("Should update details and keep agent if team remains the same")
    void shouldUpdateDetailsWithoutTeamChange() {
        // given
        Ticket ticket = createBaseTicket(TicketStatus.NEW);
        ticket.assignToAgent(agentId);

        // when
        ticket.updateDetails("New title", TicketPriority.CRITICAL, teamId);

        // then
        assertThat(ticket.getTitle()).isEqualTo("New title");
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.CRITICAL);
        assertThat(ticket.getAssignedTeamId()).isEqualTo(teamId);
        assertThat(ticket.getAssignedAgentId()).isEqualTo(agentId);
    }

    @Test
    @DisplayName("Should unassign agent if target team is changed during update")
    void shouldUnassignAgentOnTeamChange() {
        // given
        Ticket ticket = createBaseTicket(TicketStatus.NEW);
        ticket.assignToAgent(agentId);
        UUID newTeamId = UUID.randomUUID();

        // when
        ticket.updateDetails(null, null, newTeamId);

        // then
        assertThat(ticket.getAssignedTeamId()).isEqualTo(newTeamId);
        assertThat(ticket.getAssignedAgentId()).isNull();
    }

    @Test
    @DisplayName("Should throw exception on any modification attempt if ticket is CLOSED")
    void shouldFailToUpdateClosedTicket() {
        // given
        Ticket ticket = createBaseTicket(TicketStatus.CLOSED);

        // when
        Throwable thrown = catchThrowable(
                () -> ticket.updateDetails("New title", TicketPriority.HIGH, UUID.randomUUID())
        );

        // then
        assertThat(thrown)
                .isInstanceOf(TicketClosedException.class)
                .hasMessageContaining("is CLOSED and cannot be modified");
    }

    private Ticket createBaseTicket(TicketStatus status) {
        return Ticket.builder()
                .ticketNumber("INC0000001")
                .title("Test ticket")
                .description("Desc")
                .priority(TicketPriority.LOW)
                .status(status)
                .creatorId(creatorId)
                .assignedTeamId(teamId)
                .build();
    }
}
