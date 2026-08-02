package com.nn.ticketapp_api.ticket.repository;

import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    @Query(value = "SELECT nextval('ticket_number_seq')", nativeQuery = true)
    Long getNextTicketNumberSequence();
    List<Ticket> findAllByCreatorIdOrderByCreatedAtDesc(UUID creatorId);
    Page<Ticket> findByStatusAndAssignedTeamIdAndAssignedAgentIdIsNull(
            TicketStatus status,
            UUID assignedTeamId,
            Pageable pageable
    );
    Page<Ticket> findAllByAssignedAgentIdAndStatusIn(
            UUID assignedAgentId,
            List<TicketStatus> statuses,
            Pageable pageable
    );
    long countByAssignedAgentIdAndStatus(UUID assignedAgentId, TicketStatus status);
}
