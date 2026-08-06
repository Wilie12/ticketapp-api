package com.nn.ticketapp_api.ticket.repository;

import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.repository.projection.AgentStatsProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface AgentStatsRepository extends Repository<Ticket, UUID> {
    @Query(value = """
              SELECT
                  t.assigned_agent_id AS agentId,
                  COUNT(t.id) FILTER (WHERE t.status = 'IN_PROGRESS') AS currentOpenTicketsCount,
                  COUNT(t.id) FILTER (WHERE t.status IN ('RESOLVED', 'CLOSED')) AS allResolvedTicketsCount,
                  COUNT(t.id) FILTER (WHERE
                      (t.resolved_at IS NOT NULL AND t.resolved_at > t.sla_deadline) OR
                      (t.resolved_at IS NULL AND :currentTime > t.sla_deadline)
                  ) AS allSlaBreachedCount
             FROM tickets t
             WHERE t.assigned_agent_id = :agentId
             GROUP BY t.assigned_agent_id
            """, nativeQuery = true)
    Optional<AgentStatsProjection> findStatsByAgentId(
            @Param("agentId") UUID agentId,
            @Param("currentTime")Instant currentTime
    );

    @Query(value = """
            SELECT
                t.assigned_agent_id AS agentId,
                  COUNT(t.id) FILTER (WHERE t.status = 'IN_PROGRESS') AS currentOpenTicketsCount,
                  COUNT(t.id) FILTER (WHERE t.status IN ('RESOLVED', 'CLOSED')) AS allResolvedTicketsCount,
                  COUNT(t.id) FILTER (WHERE
                      (t.resolved_at IS NOT NULL AND t.resolved_at > t.sla_deadline) OR
                      (t.resolved_at IS NULL AND :currentTime > t.sla_deadline)
                  ) AS allSlaBreachedCount
            FROM tickets t
            WHERE t.assigned_agent_id IS NOT NULL
            GROUP BY t.assigned_agent_id
            """, nativeQuery = true)
    List<AgentStatsProjection> findAllAgentStats(@Param("currentTime")Instant currentTime);
}
