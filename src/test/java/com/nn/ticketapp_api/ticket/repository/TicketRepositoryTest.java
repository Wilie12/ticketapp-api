package com.nn.ticketapp_api.ticket.repository;

import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TicketRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should fetch the next sequence value from PostgreSQL")
    void shouldFetchNextTicketNumberSequence() {
        Long firstValue = ticketRepository.getNextTicketNumberSequence();
        Long secondValue = ticketRepository.getNextTicketNumberSequence();

        assertThat(firstValue).isNotNull().isPositive();
        assertThat(secondValue).isNotNull().isGreaterThan(firstValue);
    }

    @Test
    @DisplayName("Should return tickets created by a user in descending order (newest first)")
    void shouldFindAllByCreatorIdOrderByCreatedAtDesc() throws InterruptedException {
        UUID targetCreatorId = UUID.randomUUID();
        UUID otherCreatorId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        Ticket oldTicket = Ticket.createNew(
                "INC0000001", "Old", "Desc", TicketPriority.LOW, targetCreatorId, teamId);
        ticketRepository.saveAndFlush(oldTicket);
        // TODO - check if needed
        Thread.sleep(10);

        Ticket newTicket = Ticket.createNew(
                "INC0000002", "New", "Desc", TicketPriority.HIGH, targetCreatorId, teamId);
        ticketRepository.saveAndFlush(newTicket);

        Ticket otherTicket = Ticket.createNew(
                "INC0000003", "Other", "Desc", TicketPriority.MEDIUM, otherCreatorId, teamId);
        ticketRepository.saveAndFlush(otherTicket);

        List<Ticket> tickets = ticketRepository.findAllByCreatorIdOrderByCreatedAtDesc(targetCreatorId);

        assertThat(tickets).hasSize(2);
        assertThat(tickets.get(0).getTicketNumber()).isEqualTo("INC0000002");
        assertThat(tickets.get(1).getTicketNumber()).isEqualTo("INC0000001");
    }

    @Test
    @DisplayName("Should return unassigned tickets for a specific team in ascending order (oldest first)")
    void shouldFindTeamFifoQueue() throws InterruptedException {
        UUID targetTeamId = UUID.randomUUID();
        UUID otherTeamId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        Ticket firstQueueTicket = Ticket.createNew(
                "INC0000001", "T1", "Desc", TicketPriority.LOW, creatorId, targetTeamId);
        ticketRepository.saveAndFlush(firstQueueTicket);
        Thread.sleep(10);

        Ticket secondQueueTicket = Ticket.createNew(
                "INC0000002", "T2", "Desc", TicketPriority.LOW, creatorId, targetTeamId);
        ticketRepository.saveAndFlush(secondQueueTicket);

        Ticket assignedTicket = Ticket.createNew(
                "INC0000003", "T3", "Desc", TicketPriority.LOW, creatorId, targetTeamId);
        assignedTicket.setAssignedAgentId(UUID.randomUUID());
        ticketRepository.saveAndFlush(assignedTicket);

        Ticket otherTeamTicket = Ticket.createNew(
                "INC0000004", "T4", "Desc", TicketPriority.LOW, creatorId, otherTeamId);
        ticketRepository.saveAndFlush(otherTeamTicket);

        List<Ticket> queue = ticketRepository.findByStatusAndAssignedTeamIdAndAssignedAgentIdIsNullOrderByCreatedAtAsc(
                TicketStatus.NEW, targetTeamId
        );

        assertThat(queue).hasSize(2);
        assertThat(queue.get(0).getTicketNumber()).isEqualTo("INC0000001");
        assertThat(queue.get(1).getTicketNumber()).isEqualTo("INC0000002");
    }

    @Test
    @DisplayName("Should return agent's tickets matching specific statuses in descending order")
    void shouldFindAgentBacklogByStatuses() throws InterruptedException {
        UUID agentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        Ticket inProgressTicket = Ticket.createNew(
                "INC0000001", "T1", "Desc", TicketPriority.LOW, creatorId, teamId);
        inProgressTicket.setAssignedAgentId(agentId);
        inProgressTicket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.saveAndFlush(inProgressTicket);
        Thread.sleep(10);

        Ticket resolvedTicket = Ticket.createNew(
                "INC0000002", "T2", "Desc", TicketPriority.LOW, creatorId, teamId);
        resolvedTicket.setAssignedAgentId(agentId);
        resolvedTicket.setStatus(TicketStatus.RESOLVED);
        ticketRepository.saveAndFlush(resolvedTicket);

        Ticket closedTicket = Ticket.createNew(
                "INC0000003", "T3", "Desc", TicketPriority.LOW, creatorId, teamId);
        closedTicket.setAssignedAgentId(agentId);
        closedTicket.setStatus(TicketStatus.CLOSED);
        ticketRepository.saveAndFlush(closedTicket);

        List<TicketStatus> activeStatuses = List.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);
        List<Ticket> backlog = ticketRepository.findAllByAssignedAgentIdAndStatusInOrderByCreatedAtDesc(
                agentId, activeStatuses
        );

        assertThat(backlog).hasSize(2);
        assertThat(backlog.get(0).getTicketNumber()).isEqualTo("INC0000002");
        assertThat(backlog.get(1).getTicketNumber()).isEqualTo("INC0000001");
        assertThat(backlog).extracting(Ticket::getStatus).doesNotContain(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("Should calculate statistics accurately for an agent based on status")
    void shouldCountAgentTicketsByStatus() {
        UUID agentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            Ticket resolvedTicket = Ticket.createNew(
                    "INC" + i, "T" + i, "Desc", TicketPriority.LOW, creatorId, teamId);
            resolvedTicket.setAssignedAgentId(agentId);
            resolvedTicket.setStatus(TicketStatus.RESOLVED);
            ticketRepository.saveAndFlush(resolvedTicket);
        }

        Ticket inProgressTicket = Ticket.createNew(
                "INC0000001", "T1", "Desc", TicketPriority.LOW, creatorId, teamId);
        inProgressTicket.setAssignedAgentId(agentId);
        inProgressTicket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.saveAndFlush(inProgressTicket);

        long resolvedCount = ticketRepository.countByAssignedAgentIdAndStatus(agentId, TicketStatus.RESOLVED);
        long inProgressCount = ticketRepository.countByAssignedAgentIdAndStatus(agentId, TicketStatus.IN_PROGRESS);
        long newCount = ticketRepository.countByAssignedAgentIdAndStatus(agentId, TicketStatus.NEW);

        assertThat(resolvedCount).isEqualTo(3);
        assertThat(inProgressCount).isEqualTo(1);
        assertThat(newCount).isEqualTo(0);
    }
}
