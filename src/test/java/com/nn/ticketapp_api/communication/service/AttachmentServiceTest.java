package com.nn.ticketapp_api.communication.service;

import com.nn.ticketapp_api.communication.api.mapper.AttachmentMapper;
import com.nn.ticketapp_api.communication.api.response.AttachmentResponse;
import com.nn.ticketapp_api.communication.domain.Attachment;
import com.nn.ticketapp_api.communication.exception.AttachmentOwnershipException;
import com.nn.ticketapp_api.communication.exception.InvalidAttachmentException;
import com.nn.ticketapp_api.communication.repository.AttachmentRepository;
import com.nn.ticketapp_api.shared.storage.service.StorageService;
import com.nn.ticketapp_api.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private TicketService ticketService;
    @Mock
    private AttachmentMapper attachmentMapper;
    @InjectMocks
    private AttachmentService attachmentService;

    @Test
    @DisplayName("Should successfully upload attachment to active ticket")
    void shouldUploadAttachmentSuccessfully() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "error_log.txt",
                "text/plain",
                "test content".getBytes()
        );

        Attachment mockAttachment = Attachment.create(
                ticketId,
                authorId,
                "error_log.txt",
                "tickets/key.txt",
                "text/plain",
                12L
        );
        AttachmentResponse mockResponse = new AttachmentResponse(
                UUID.randomUUID(),
                "error_log.txt",
                "http://localhost:9000/bucket/tickets/key.txt",
                Instant.now()
        );

        given(attachmentRepository.save(any(Attachment.class))).willReturn(mockAttachment);
        given(attachmentMapper.toResponse(mockAttachment)).willReturn(mockResponse);

        // when
        AttachmentResponse response = attachmentService.uploadAttachment(ticketId, authorId, file);

        // then
        assertThat(response).isNotNull();
        assertThat(response.filename()).isEqualTo("error_log.txt");

        then(ticketService).should().ensureTicketIsActive(ticketId);
        then(storageService).should().uploadFile(any(), any(), eq("text/plain"), anyLong());
        then(attachmentRepository).should().save(any(Attachment.class));
        then(attachmentMapper).should().toResponse(mockAttachment);
    }

    @Test
    @DisplayName("Should throw InvalidAttachmentException when uploading empty file")
    void shouldRejectEmptyFile() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "",
                "text/plain",
                new byte[0]
        );

        // when
        Throwable thrown = catchThrowable(() -> attachmentService.uploadAttachment(ticketId, authorId, emptyFile));

        // then
        assertThat(thrown)
                .isInstanceOf(InvalidAttachmentException.class)
                .hasMessageContaining("Cannot upload an empty file");

        then(ticketService).should().ensureTicketIsActive(ticketId);
        then(storageService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should throw InvalidAttachmentException on path traversal attempt in filename")
    void shouldRejectPathTraversalFilename() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "../../etc/passwd",
                "text/plain",
                "malicious".getBytes()
        );

        // when
        Throwable thrown = catchThrowable(() -> attachmentService.uploadAttachment(ticketId, authorId, maliciousFile));

        assertThat(thrown)
                .isInstanceOf(InvalidAttachmentException.class)
                .hasMessageContaining("Filename contains invalid path sequence");

        then(ticketService).should().ensureTicketIsActive(ticketId);
        then(storageService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should successfully delete attachment when requester is the owner")
    void shouldDeleteAttachmentSuccessfully() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String objectKey = "tickets/file.pdf";

        Attachment mockAttachment = Attachment.create(
                ticketId,
                ownerId,
                "file.pdf",
                objectKey,
                "application/pdf",
                1024L
        );

        given(attachmentRepository.findById(attachmentId)).willReturn(Optional.of(mockAttachment));

        // when
        attachmentService.deleteAttachment(ticketId, attachmentId, ownerId);

        // then
        then(ticketService).should().ensureTicketIsActive(ticketId);
        then(storageService).should().deleteFile(objectKey);
        then(attachmentRepository).should().delete(mockAttachment);
    }

    @Test
    @DisplayName("Should throw AttachmentOwnershipException when non-owner tries to delete attachment")
    void shouldThrowExceptionWhenNonOwnerTriesToDeleteAttachment() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID maliciousUserId = UUID.randomUUID();

        Attachment mockAttachment = Attachment.create(
                ticketId,
                ownerId,
                "file.pdf",
                "tickets/file.pdf",
                "application/pdf",
                1024L
        );

        given(attachmentRepository.findById(attachmentId)).willReturn(Optional.of(mockAttachment));

        // when
        Throwable thrown  = catchThrowable(
                () -> attachmentService.deleteAttachment(ticketId, attachmentId, maliciousUserId)
        );

        assertThat(thrown)
                .isInstanceOf(AttachmentOwnershipException.class)
                .hasMessageContaining("does not have permission to modify attachment");

        then(ticketService).should().ensureTicketIsActive(ticketId);
        then(storageService).shouldHaveNoInteractions();
        then(attachmentRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("Should throw InvalidAttachmentException when attachment ticketId does not match URL path ticketId")
    void shouldPreventPathSpoofingDuringDeletion() {
        // given
        UUID pathTicketId = UUID.randomUUID();
        UUID actualTicketId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Attachment mockAttachment = Attachment.create(
                actualTicketId,
                ownerId,
                "file.pdf",
                "tickets/file.pdf",
                "application/pdf",
                1024L
        );

        given(attachmentRepository.findById(attachmentId)).willReturn(Optional.of(mockAttachment));

        // when
        Throwable thrown = catchThrowable(
                () -> attachmentService.deleteAttachment(pathTicketId, attachmentId, ownerId)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(InvalidAttachmentException.class)
                .hasMessageContaining("Attachment does not belong to the specified ticket path");

        then(ticketService).should().ensureTicketIsActive(pathTicketId);
        then(storageService).shouldHaveNoInteractions();
        then(attachmentRepository).should(never()).delete(any());
    }
}
