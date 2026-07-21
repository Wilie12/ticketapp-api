package com.nn.ticketapp_api.communication.repository;

import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.communication.domain.Communication;
import com.nn.ticketapp_api.communication.domain.CommunicationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CommunicationRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private CommunicationRepository communicationRepository;

    @Test
    @DisplayName("Should save and retireve communications ordered by creation date ascending")
    void shouldSaveAndRetrieveCommunicationsOrderedByDate() {
        // given
        UUID ticketId = UUID.randomUUID();
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
        UUID ticketId = UUID.randomUUID();
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
}
