package com.nn.ticketapp_api.ticket.domain;

import java.util.Set;

public enum TicketStatus {
    NEW,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public boolean canTransitionTo(TicketStatus targetStatus) {
        return switch (this) {
            case NEW, RESOLVED -> Set.of(IN_PROGRESS, CLOSED).contains(targetStatus);
            case IN_PROGRESS -> Set.of(RESOLVED, CLOSED).contains(targetStatus);
            case CLOSED -> false;
        };
    }
}
