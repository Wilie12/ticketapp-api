package com.nn.ticketapp_api.ticket.repository;

import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
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
    List<Ticket> findByStatusAndAssignedTeamIdAndAssignedAgentIdIsNullOrderByCreatedAtAsc(
            TicketStatus status,
            UUID assignedTeamId
    );
    List<Ticket> findAllByAssignedAgentIdAndStatusInOrderByCreatedAtDesc(
            UUID assignedAgentId,
            List<TicketStatus> statuses
    );
    long countByAssignedAgentIdAndStatus(UUID assignedAgentId, TicketStatus status);
}
