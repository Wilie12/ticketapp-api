package com.nn.ticketapp_api.ticket.domain;

import com.nn.ticketapp_api.ticket.exception.InvalidStatusTransitionException;
import com.nn.ticketapp_api.ticket.exception.TicketClosedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TicketTest {
    private final UUID creatorId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID agentId = UUID.randomUUID();

    @Test
    @DisplayName("Should create a NEW ticket with correct initial state")
    void shouldCreateNewTicket() {
        Ticket ticket = Ticket.createNew(
                "INC0000001", "Test ticket", "Desc", TicketPriority.LOW, creatorId, teamId
        );

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(ticket.getTicketNumber()).isEqualTo("INC0000001");
        assertThat(ticket.getCreatorId()).isEqualTo(creatorId);
        assertThat(ticket.getAssignedTeamId()).isEqualTo(teamId);
        assertThat(ticket.getAssignedAgentId()).isNull();
    }

    @Test
    @DisplayName("Should successfully assign a NEW ticket to an agent")
    void shouldAssignToAgent() {
        Ticket ticket = createBaseTicket(TicketStatus.NEW);

        ticket.assignToAgent(agentId);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getAssignedAgentId()).isEqualTo(agentId);
    }

    @Test
    @DisplayName("Should throw exception when trying to assign ticket that is not NEW")
    void shouldFailToAssignIfTicketIsNotNew() {
        Ticket ticket = createBaseTicket(TicketStatus.IN_PROGRESS);

        assertThatThrownBy(() -> ticket.assignToAgent(agentId))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Only NEW tickets can be assigned");
    }

    @Test
    @DisplayName("Should successfully resolve an IN_PROGRESS ticket")
    void shouldResolveInProgressTicket() {
        Ticket ticket = createBaseTicket(TicketStatus.IN_PROGRESS);

        ticket.resolve();

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should successfully close a RESOLVED ticket")
    void shouldCloseTicket() {
        Ticket ticket = createBaseTicket(TicketStatus.RESOLVED);

        ticket.close();

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("Should successfully reopen a RESOLVED ticket and clear resolvedAt")
    void shouldReopenTicket() {
        Ticket ticket = createBaseTicket(TicketStatus.IN_PROGRESS);
        ticket.resolve();

        ticket.reopen();

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("Should block reopening of a CLOSED ticket")
    void shouldFailToReopenClosedTicket() {
        Ticket ticket = createBaseTicket(TicketStatus.CLOSED);

        assertThatThrownBy(ticket::reopen)
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Only RESOLVED tickets can be reopened");
    }

    @Test
    @DisplayName("Should update details and keep agent if team remains the same")
    void shouldUpdateDetailsWithoutTeamChange() {
        Ticket ticket = createBaseTicket(TicketStatus.NEW);
        ticket.assignToAgent(agentId);

        ticket.updateDetails("New title", TicketPriority.CRITICAL, teamId);

        assertThat(ticket.getTitle()).isEqualTo("New title");
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.CRITICAL);
        assertThat(ticket.getAssignedTeamId()).isEqualTo(teamId);
        assertThat(ticket.getAssignedAgentId()).isEqualTo(agentId);
    }

    @Test
    @DisplayName("Should unassign agent if target team is changed during update")
    void shouldUnassignAgentOnTeamChange() {
        Ticket ticket = createBaseTicket(TicketStatus.NEW);
        ticket.assignToAgent(agentId);
        UUID newTeamId = UUID.randomUUID();

        ticket.updateDetails(null, null, newTeamId);

        assertThat(ticket.getAssignedTeamId()).isEqualTo(newTeamId);
        assertThat(ticket.getAssignedAgentId()).isNull();
    }

    @Test
    @DisplayName("Should throw exception on any modification attempt if ticket is CLOSED")
    void shouldFailToUpdateClosedTicket() {
        Ticket ticket = createBaseTicket(TicketStatus.CLOSED);

        assertThatThrownBy(() -> ticket.updateDetails("New title", TicketPriority.HIGH, UUID.randomUUID()))
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
