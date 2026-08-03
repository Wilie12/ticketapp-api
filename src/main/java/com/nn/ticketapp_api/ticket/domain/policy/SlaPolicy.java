package com.nn.ticketapp_api.ticket.domain.policy;

import com.nn.ticketapp_api.ticket.domain.TicketPriority;

import java.time.Instant;

public interface SlaPolicy {
    Instant calculateDeadline(TicketPriority ticketPriority, Instant creationTime);
}
