package com.nn.ticketapp_api.communication.service;

import com.nn.ticketapp_api.communication.api.mapper.AttachmentMapper;
import com.nn.ticketapp_api.communication.api.response.AttachmentResponse;
import com.nn.ticketapp_api.communication.domain.Attachment;
import com.nn.ticketapp_api.communication.exception.AttachmentNotFoundException;
import com.nn.ticketapp_api.communication.exception.AttachmentOwnershipException;
import com.nn.ticketapp_api.communication.exception.InvalidAttachmentException;
import com.nn.ticketapp_api.communication.repository.AttachmentRepository;
import com.nn.ticketapp_api.shared.storage.service.StorageService;
import com.nn.ticketapp_api.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final StorageService storageService;
    private final TicketService ticketService;
    private final AttachmentMapper attachmentMapper;

    @Transactional
    public AttachmentResponse uploadAttachment(UUID ticketId, UUID authorId, MultipartFile file) {
        log.debug("Uploading attachment for ticket {} by user {}", ticketId, authorId);

        ticketService.ensureTicketIsActive(ticketId);

        if (file == null || file.isEmpty()) {
            throw new InvalidAttachmentException("Cannot upload an empty file");
        }

        String originalFilename = StringUtils.cleanPath(
                Objects.requireNonNullElse(file.getOriginalFilename(), "unknown")
        );
        if (originalFilename.contains("..")) {
            throw new InvalidAttachmentException("Filename contains invalid path sequence");
        }

        String objectKey = String.format("tickets/%s/%s_%s", ticketId, UUID.randomUUID(), originalFilename);

        try {
            storageService.uploadFile(objectKey, file.getInputStream(), file.getContentType(), file.getSize());
        } catch (IOException e) {
            log.error("Failed to read input stream from uploaded file", e);
            throw new InvalidAttachmentException("Could not process the uploaded file");
        }

        Attachment attachment = Attachment.create(
                ticketId,
                authorId,
                originalFilename,
                objectKey,
                file.getContentType(),
                file.getSize()
        );

        Attachment savedAttachment = attachmentRepository.save(attachment);
        log.info("Successfully uploaded and saved metadata for attachment: {}", objectKey);

        return attachmentMapper.toResponse(savedAttachment);
    }

    @Transactional
    public void deleteAttachment(UUID ticketId, UUID attachmentId, UUID requesterId) {
        log.debug("Request to delete attachment {} from ticket {} by user {}", attachmentId, ticketId, requesterId);

        ticketService.ensureTicketIsActive(ticketId);

        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentNotFoundException(attachmentId));

        if (!attachment.getTicketId().equals(ticketId)) {
            log.warn(
                    "Security violation: Attempt to delete attachment {} mapped to a different ticket that {}",
                    attachmentId,
                    ticketId
            );
            throw new InvalidAttachmentException("Attachment does not belong to the specified ticket path");
        }

        if (!attachment.getAuthorId().equals(requesterId)) {
            log.warn(
                    "Security violation: User {} attempted to delete attachment {} without ownership",
                    requesterId,
                    attachmentId
            );
            throw new AttachmentOwnershipException(attachmentId, requesterId);
        }

        storageService.deleteFile(attachment.getObjectKey());
        attachmentRepository.delete(attachment);

        log.info("Successfully deleted attachment metadata and minio object for key: {}", attachment.getObjectKey());
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getTicketAttachments(UUID ticketId) {
        log.debug("Retrieving all attachments for ticket {}", ticketId);

        return attachmentRepository.findByTicketIdOrderByUploadedAtAsc(ticketId)
                .stream()
                .map(attachmentMapper::toResponse)
                .toList();
    }

}
