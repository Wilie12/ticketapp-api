package com.nn.ticketapp_api.ticket.listener;

import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TicketMetricsListenerTest {

    private MeterRegistry meterRegistry;
    private TicketMetricsListener ticketMetricsListener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        ticketMetricsListener = new TicketMetricsListener(meterRegistry);
    }

    @Test
    @DisplayName("Should increment tickets.created.total counter with team_id tag on TicketCreatedEvent")
    void shouldIncrementCreatedCounterOnTicketCreatedEvent() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        TicketCreatedEvent ticketCreatedEvent = new TicketCreatedEvent(ticketId, teamId);

        // when
        ticketMetricsListener.onTicketCreated(ticketCreatedEvent);

        // when
        double count = meterRegistry
                .counter("tickets.created.total", "team_id", teamId.toString())
                .count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment tickets.resolved.total counter with team_id tag on TicketResolveEvent")
    void shouldIncrementResolvedCounterOnTicketResolvedEvent() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        TicketResolvedEvent ticketResolvedEvent = new TicketResolvedEvent(
                ticketId,
                agentId,
                teamId,
                "Fixed issue"
        );

        // when
        ticketMetricsListener.onTicketResolved(ticketResolvedEvent);

        // when
        double count = meterRegistry
                .counter("tickets.resolved.total", "team_id", teamId.toString())
                .count();
        assertThat(count).isEqualTo(1.0);
    }
}
