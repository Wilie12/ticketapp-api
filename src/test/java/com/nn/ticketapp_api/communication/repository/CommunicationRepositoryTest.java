package com.nn.ticketapp_api.communication.repository;

import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.communication.domain.Communication;
import com.nn.ticketapp_api.communication.domain.CommunicationType;
import com.nn.ticketapp_api.team.domain.Team;
import com.nn.ticketapp_api.team.repository.TeamRepository;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CommunicationRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private CommunicationRepository communicationRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private TeamRepository teamRepository;

    @AfterEach
    void tearDown() {
        communicationRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should save and retrieve communications ordered by creation date ascending")
    void shouldSaveAndRetrieveCommunicationsOrderedByDate() {
        // given
        UUID ticketId = setupActiveTicket();
        UUID authorId = UUID.randomUUID();

        Communication comment = Communication.createComment(ticketId, authorId, "Public comment.");
        Communication workNote = Communication.createWorkNote(ticketId, authorId, "Work note.");

        communicationRepository.saveAll(List.of(comment, workNote));

        // when
        List<Communication> results = communicationRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Communication::getType)
                .containsExactlyInAnyOrder(CommunicationType.PUBLIC_COMMENT, CommunicationType.WORK_NOTE);
    }

    @Test
    @DisplayName("Should retrieve only specific type of communications for a given ticket")
    void shouldFilterCommunicationByType() {
        // given
        UUID ticketId = setupActiveTicket();
        UUID authorId = UUID.randomUUID();

        Communication comment = Communication.createComment(ticketId, authorId, "Public comment.");
        Communication workNote = Communication.createWorkNote(ticketId, authorId, "Work note.");

        communicationRepository.saveAll(List.of(comment, workNote));

        // when
        List<Communication> publicComments = communicationRepository.findByTicketIdAndTypeOrderByCreatedAtAsc(
                ticketId,
                CommunicationType.PUBLIC_COMMENT
        );

        // then
        assertThat(publicComments).hasSize(1);
        assertThat(publicComments.get(0).getType()).isEqualTo(CommunicationType.PUBLIC_COMMENT);
    }

    private UUID setupActiveTicket() {
        Team team = Team.create("Communication Team", "Desc");
        teamRepository.saveAndFlush(team);

        Ticket ticket = Ticket.builder()
                .ticketNumber("INC0000001")
                .title("Communication Ticket")
                .description("Test")
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.NEW)
                .creatorId(UUID.randomUUID())
                .assignedTeamId(team.getId())
                .build();
        ticketRepository.saveAndFlush(ticket);

        return ticket.getId();
    }
}
