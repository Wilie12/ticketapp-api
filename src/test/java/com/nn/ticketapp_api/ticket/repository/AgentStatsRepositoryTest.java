package com.nn.ticketapp_api.ticket.repository;

import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.repository.projection.AgentStatsProjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AgentStatsRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private AgentStatsRepository agentStatsRepository;
    @Autowired
    private TicketRepository ticketRepository;

    private final Instant fixedNow = Instant.parse("2026-08-06T14:00:00Z");
    private final Clock fixedClock = Clock.fixed(fixedNow, ZoneId.of("UTC"));

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should accurately aggregate agent statistics")
    void shouldAggregateAgentStatisticsAccurately() {
        // given
        UUID targetAgentId = UUID.randomUUID();
        UUID otherAgentId = UUID.randomUUID();
        Instant now = Instant.now(fixedClock);

        Ticket firstTicket = buildTicket(
                "INC0000010",
                TicketStatus.IN_PROGRESS,
                targetAgentId,
                now.plus(1, ChronoUnit.HOURS),
                null
        );
        ticketRepository.save(firstTicket);

        Ticket secondTicket = buildTicket(
                "INC0000011",
                TicketStatus.IN_PROGRESS,
                targetAgentId,
                now.minus(1, ChronoUnit.HOURS),
                null
        );
        ticketRepository.save(secondTicket);

        Ticket thirdTicket = buildTicket(
                "INC0000012",
                TicketStatus.RESOLVED,
                targetAgentId,
                now.plus(2, ChronoUnit.HOURS),
                now
        );
        ticketRepository.save(thirdTicket);

        Ticket fourthticket = buildTicket(
                "INC0000013",
                TicketStatus.CLOSED,
                targetAgentId,
                now.minus(2, ChronoUnit.HOURS),
                now.minus(1, ChronoUnit.HOURS)
        );
        ticketRepository.save(fourthticket);

        Ticket fifthTicket = buildTicket(
                "INC0000014",
                TicketStatus.NEW,
                null,
                now.plus(1, ChronoUnit.HOURS),
                null
        );
        ticketRepository.save(fifthTicket);

        Ticket sixthTicket = buildTicket(
                "INC0000015",
                TicketStatus.IN_PROGRESS,
                otherAgentId,
                now.minus(1, ChronoUnit.HOURS),
                null
        );
        ticketRepository.save(sixthTicket);

        ticketRepository.flush();

        // when
        Optional<AgentStatsProjection> statsResult = agentStatsRepository.findStatsByAgentId(targetAgentId, now);
        List<AgentStatsProjection> allStatsResult = agentStatsRepository.findAllAgentStats(now);

        // then
        assertThat(statsResult).isPresent();
        AgentStatsProjection stats = statsResult.get();

        assertThat(stats.getAgentId()).isEqualTo(targetAgentId);
        assertThat(stats.getCurrentOpenTicketsCount()).isEqualTo(2);
        assertThat(stats.getAllResolvedTicketsCount()).isEqualTo(2);
        assertThat(stats.getAllSlaBreachedCount()).isEqualTo(2);

        assertThat(allStatsResult).hasSize(2);
    }

    private Ticket buildTicket(
            String ticketNumber,
            TicketStatus status,
            UUID agentId,
            Instant slaDeadline,
            Instant resolvedAt
    ) {
        return Ticket.builder()
                .ticketNumber(ticketNumber)
                .title("Stats validation " + ticketNumber)
                .description("Performance test payload")
                .priority(TicketPriority.HIGH)
                .status(status)
                .creatorId(UUID.randomUUID())
                .assignedAgentId(UUID.randomUUID())
                .assignedAgentId(agentId)
                .slaDeadline(slaDeadline)
                .resolvedAt(resolvedAt)
                .build();
    }
}
