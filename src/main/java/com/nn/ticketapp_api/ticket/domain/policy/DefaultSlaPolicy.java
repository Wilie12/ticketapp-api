package com.nn.ticketapp_api.ticket.domain.policy;

import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DefaultSlaPolicy implements SlaPolicy {

    @Override
    public Instant calculateDeadline(TicketPriority ticketPriority, Instant creationTime) {
        Duration slaDuration = switch (ticketPriority) {
            case CRITICAL -> Duration.ofHours(2);
            case HIGH -> Duration.ofHours(4);
            case MEDIUM -> Duration.ofHours(24);
            case LOW -> Duration.ofHours(48);
        };

        return creationTime.plus(slaDuration);
    }
}
