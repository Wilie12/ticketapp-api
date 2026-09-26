package com.nn.ticketapp_api.ticket.listener;

import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketMetricsListener {

    private static final String TICKETS_CREATED_METRIC = "tickets.created.total";
    private static final String TICKETS_RESOLVED_METRIC = "tickets.resolved.total";
    private static final String TAG_TEAM_ID = "team_id";

    private final MeterRegistry meterRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(TicketCreatedEvent event) {
        log.debug("Recording created metric for ticket {} in team {}", event.ticketId(), event.teamId());

        meterRegistry.counter(TICKETS_CREATED_METRIC, TAG_TEAM_ID, event.teamId().toString()).increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketResolved(TicketResolvedEvent event) {
        log.debug("Recording resolved metric for ticket {} in team {}", event.ticketId(), event.teamId());

        meterRegistry.counter(TICKETS_RESOLVED_METRIC, TAG_TEAM_ID, event.teamId().toString()).increment();
    }
}
