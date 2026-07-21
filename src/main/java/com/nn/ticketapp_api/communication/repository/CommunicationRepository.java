package com.nn.ticketapp_api.communication.repository;

import com.nn.ticketapp_api.communication.domain.Communication;
import com.nn.ticketapp_api.communication.domain.CommunicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommunicationRepository extends JpaRepository<Communication, UUID> {
    List<Communication> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
    List<Communication> findByTicketIdAndTypeOrderByCreatedAtAsc(UUID ticketId, CommunicationType type);
}
