package com.nn.ticketapp_api.communication.repository;

import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.communication.domain.Attachment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AttachmentRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Test
    @DisplayName("Should successfully persist attachment metadata and retireve them ordered by upload date")
    void shouldPersistAndRetrieveAttachmentMetadata() throws InterruptedException {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        Attachment firstAttachment = Attachment.create(
                ticketId,
                authorId,
                "error_log.txt",
                "tickets/log1.txt",
                "text/plain",
                1024L
        );
        attachmentRepository.saveAndFlush(firstAttachment);

        Thread.sleep(10);

        Attachment secondAttachment = Attachment.create(
                ticketId,
                authorId,
                "screenshot.png",
                "tickets/screen1.png",
                "image/png",
                2048L
        );
        attachmentRepository.saveAndFlush(secondAttachment);

        // when
        List<Attachment> results = attachmentRepository.findByTicketIdOrderByUploadedAtAsc(ticketId);

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getOriginalFilename()).isEqualTo("error_log.txt");
        assertThat(results.get(1).getOriginalFilename()).isEqualTo("screenshot.png");
    }
}
