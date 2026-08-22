package com.nn.ticketapp_api.ticket.repository;

import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.team.domain.Team;
import com.nn.ticketapp_api.team.repository.TeamRepository;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TicketRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private TeamRepository teamRepository;

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should fetch the next sequence value from PostgreSQL")
    void shouldFetchNextTicketNumberSequence() {
        // given (state is managed by the sequence in DB)

        // when
        Long firstValue = ticketRepository.getNextTicketNumberSequence();
        Long secondValue = ticketRepository.getNextTicketNumberSequence();

        // then
        assertThat(firstValue).isNotNull().isPositive();
        assertThat(secondValue).isNotNull().isGreaterThan(firstValue);
    }

    @Test
    @DisplayName("Should accurately persist and retrieve Instant fields mapping to TIMESTAMP WITH TIME ZONE")
    void shouldPersistTimeFieldsAccurately() {
        // given
        Team team = Team.create("Time Team", "Desc");
        teamRepository.saveAndFlush(team);

        Instant expectedSlaDeadline = Instant.parse("2026-08-03T12:00:00Z");
        Instant expectedResolvedAt = Instant.parse("2026-08-04T12:00:00Z");

        Ticket ticket = Ticket.builder()
                .ticketNumber("INC0000001")
                .title("Test Ticket")
                .description("Testing time boundaries")
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.RESOLVED)
                .creatorId(UUID.randomUUID())
                .assignedTeamId(team.getId())
                .slaDeadline(expectedSlaDeadline)
                .resolvedAt(expectedResolvedAt)
                .build();
        ticketRepository.save(ticket);

        // when
        Ticket retrievedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();

        // then
        assertThat(retrievedTicket.getSlaDeadline()).isEqualTo(expectedSlaDeadline);
        assertThat(retrievedTicket.getResolvedAt()).isEqualTo(expectedResolvedAt);
    }

    @Test
    @DisplayName("Should return tickets created by a user in descending order (newest first)")
    void shouldFindAllByCreatorIdOrderByCreatedAtDesc() throws InterruptedException {
        // given
        Team team = Team.create("Alpha Team", "Desc");
        teamRepository.saveAndFlush(team);
        UUID teamId = team.getId();

        UUID targetCreatorId = UUID.randomUUID();
        UUID otherCreatorId = UUID.randomUUID();

        Ticket oldTicket = buildTicket(
                "INC0000001",
                TicketStatus.NEW,
                targetCreatorId,
                teamId,
                null
        );
        ticketRepository.saveAndFlush(oldTicket);
        Thread.sleep(10);

        Ticket newTicket = buildTicket(
                "INC0000002",
                TicketStatus.NEW,
                targetCreatorId,
                teamId,
                null
        );
        ticketRepository.saveAndFlush(newTicket);

        Ticket otherTicket = buildTicket(
                "INC0000003",
                TicketStatus.NEW,
                otherCreatorId,
                teamId,
                null
        );
        ticketRepository.saveAndFlush(otherTicket);

        // when
        List<Ticket> tickets = ticketRepository.findAllByCreatorIdOrderByCreatedAtDesc(targetCreatorId);

        // then
        assertThat(tickets).hasSize(2);
        assertThat(tickets.get(0).getTicketNumber()).isEqualTo("INC0000002");
        assertThat(tickets.get(1).getTicketNumber()).isEqualTo("INC0000001");
    }

    @Test
    @DisplayName("Should return unassigned tickets for a specific team in ascending order (oldest first)")
    void shouldFindTeamFifoQueue() throws InterruptedException {
        // given
        Team targetTeam = Team.create("Target Team", "Desc");
        Team otherTeam = Team.create("Other Team", "Desc");
        teamRepository.saveAllAndFlush(List.of(targetTeam, otherTeam));

        UUID targetTeamId = targetTeam.getId();
        UUID otherTeamId = otherTeam.getId();
        UUID creatorId = UUID.randomUUID();

        Ticket firstQueueTicket = buildTicket(
                "INC0000001",
                TicketStatus.NEW,
                creatorId,
                targetTeamId,
                null
        );
        ticketRepository.saveAndFlush(firstQueueTicket);
        Thread.sleep(10);

        Ticket secondQueueTicket = buildTicket(
                "INC0000002",
                TicketStatus.NEW,
                creatorId,
                targetTeamId,
                null
        );
        ticketRepository.saveAndFlush(secondQueueTicket);

        Ticket assignedTicket = buildTicket(
                "INC0000003",
                TicketStatus.IN_PROGRESS,
                creatorId,
                targetTeamId,
                UUID.randomUUID()
        );
        ticketRepository.saveAndFlush(assignedTicket);

        Ticket otherTeamTicket = buildTicket(
                "INC0000004",
                TicketStatus.NEW,
                creatorId,
                otherTeamId,
                null
        );
        ticketRepository.saveAndFlush(otherTeamTicket);

        PageRequest pageRequest = PageRequest
                .of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

        // when
        Page<Ticket> queue = ticketRepository.findByStatusAndAssignedTeamIdAndAssignedAgentIdIsNull(
                TicketStatus.NEW, targetTeamId, pageRequest
        );

        // then
        assertThat(queue).hasSize(2);
        assertThat(queue.getTotalElements()).isEqualTo(2);
        assertThat(queue.getContent().get(0).getTicketNumber()).isEqualTo("INC0000001");
        assertThat(queue.getContent().get(1).getTicketNumber()).isEqualTo("INC0000002");
    }

    @Test
    @DisplayName("Should return agent's tickets matching specific statuses in descending order")
    void shouldFindAgentBacklogByStatuses() throws InterruptedException {
        // given
        Team team = Team.create("Backlog Team", "Desc");
        teamRepository.saveAndFlush(team);
        UUID teamId = team.getId();

        UUID agentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        Ticket inProgressTicket = buildTicket(
                "INC0000001",
                TicketStatus.IN_PROGRESS,
                creatorId,
                teamId,
                agentId
        );
        ticketRepository.saveAndFlush(inProgressTicket);
        Thread.sleep(10);

        Ticket resolvedTicket = buildTicket(
                "INC0000002",
                TicketStatus.RESOLVED,
                creatorId,
                teamId,
                agentId
        );
        ticketRepository.saveAndFlush(resolvedTicket);

        Ticket closedTicket = buildTicket(
                "INC0000003",
                TicketStatus.CLOSED,
                creatorId,
                teamId,
                agentId
        );
        ticketRepository.saveAndFlush(closedTicket);

        List<TicketStatus> activeStatuses = List.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);

        PageRequest pageRequest = PageRequest
                .of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Ticket> backlog = ticketRepository.findAllByAssignedAgentIdAndStatusIn(
                agentId, activeStatuses, pageRequest
        );

        // then
        assertThat(backlog).hasSize(2);
        assertThat(backlog.getTotalElements()).isEqualTo(2);
        assertThat(backlog.getContent().get(0).getTicketNumber()).isEqualTo("INC0000002");
        assertThat(backlog.getContent().get(1).getTicketNumber()).isEqualTo("INC0000001");
        assertThat(backlog.getContent()).extracting(Ticket::getStatus).doesNotContain(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("Should calculate statistics accurately for an agent based on status")
    void shouldCountAgentTicketsByStatus() {
        // given
        Team team = Team.create("Count Team", "Desc");
        teamRepository.saveAndFlush(team);
        UUID teamId = team.getId();

        UUID agentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            Ticket resolvedTicket = buildTicket(
                    "INC" + i,
                    TicketStatus.RESOLVED,
                    creatorId,
                    teamId,
                    agentId
            );
            ticketRepository.saveAndFlush(resolvedTicket);
        }

        Ticket inProgressTicket = buildTicket(
                "INC0000001",
                TicketStatus.IN_PROGRESS,
                creatorId,
                teamId,
                agentId
        );
        ticketRepository.saveAndFlush(inProgressTicket);

        // when
        long resolvedCount = ticketRepository.countByAssignedAgentIdAndStatus(agentId, TicketStatus.RESOLVED);
        long inProgressCount = ticketRepository.countByAssignedAgentIdAndStatus(agentId, TicketStatus.IN_PROGRESS);
        long newCount = ticketRepository.countByAssignedAgentIdAndStatus(agentId, TicketStatus.NEW);

        // then
        assertThat(resolvedCount).isEqualTo(3);
        assertThat(inProgressCount).isEqualTo(1);
        assertThat(newCount).isEqualTo(0);
    }

    private Ticket buildTicket(
            String ticketNumber,
            TicketStatus ticketStatus,
            UUID creatorId,
            UUID teamId,
            UUID agentId
    ) {
        return Ticket.builder()
                .ticketNumber(ticketNumber)
                .title("Title for " + ticketNumber)
                .description("Test description")
                .priority(TicketPriority.LOW)
                .status(ticketStatus)
                .creatorId(creatorId)
                .assignedTeamId(teamId)
                .assignedAgentId(agentId)
                .build();
    }
}
