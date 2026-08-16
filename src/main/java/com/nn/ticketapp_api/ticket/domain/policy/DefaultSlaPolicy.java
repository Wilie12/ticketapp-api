package com.nn.ticketapp_api.ticket.domain.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class DefaultSlaPolicy implements SlaPolicy {

    @Override
    public Instant calculateDeadline(Instant creationTime, Integer resolutionHours) {
        return creationTime.plus(resolutionHours, ChronoUnit.HOURS);
    }
}
