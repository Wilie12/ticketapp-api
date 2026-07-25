package com.nn.ticketapp_api.communication.repository;

import com.nn.ticketapp_api.communication.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByTicketIdOrderByUploadedAtAsc(UUID ticketId);
}
