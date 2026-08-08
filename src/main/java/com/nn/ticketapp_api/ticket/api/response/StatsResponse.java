package com.nn.ticketapp_api.ticket.api.response;

import java.util.UUID;

public record StatsResponse(
        UUID agentId,
        long currentOpenTicketsCount,
        long allResolvedTicketsCount,
        long allSlaBreachedCount
) {
}
