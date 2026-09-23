package com.nn.ticketapp_api.ticket.repository;

import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.repository.projection.AgentStatsProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface AgentStatsRepository extends Repository<Ticket, UUID> {
    @Query(value = "SELECT * FROM agent_stats_mv WHERE agent_id = :agentId", nativeQuery = true)
    Optional<AgentStatsProjection> findStatsByAgentId(@Param("agentId") UUID agentId);

    @Query(value = "SELECT * FROM agent_stats_mv", nativeQuery = true)
    List<AgentStatsProjection> findAllAgentStats();
}
