package com.nn.ticketapp_api.ticket.domain.policy;

import java.time.Instant;

public interface SlaPolicy {
    Instant calculateDeadline(Instant creationTime, Integer resolutionHours);
}
