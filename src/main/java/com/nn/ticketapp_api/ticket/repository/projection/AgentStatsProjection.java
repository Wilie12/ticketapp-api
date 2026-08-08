package com.nn.ticketapp_api.ticket.repository.projection;

import java.util.UUID;

public interface AgentStatsProjection {
    UUID getAgentId();
    long getCurrentOpenTicketsCount();
    long getAllResolvedTicketsCount();
    long getAllSlaBreachedCount();
}
