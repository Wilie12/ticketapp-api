package com.nn.ticketapp_api.ticket.domain.policy;

import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultSlaPolicyTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-03T10:00:00Z");
    private final SlaPolicy slaPolicy = new DefaultSlaPolicy();

    @Test
    @DisplayName("Should add 2 hours to creation time for CRITICAL priority")
    void shouldCalculateDeadlineForCriticalPriority() {
        // given
        TicketPriority priority = TicketPriority.CRITICAL;
        Instant expectedDeadline = FIXED_NOW.plus(2, ChronoUnit.HOURS);

        // when
        Instant actualDeadline = slaPolicy.calculateDeadline(priority, FIXED_NOW);

        // then
        assertThat(actualDeadline).isEqualTo(expectedDeadline);
    }

    @Test
    @DisplayName("Should add 24 hours to creation time for MEDIUM priority")
    void shouldCalculateDeadlineForMediumPriority() {
        // given
        TicketPriority priority = TicketPriority.MEDIUM;
        Instant expectedDeadline = FIXED_NOW.plus(24, ChronoUnit.HOURS);

        // when
        Instant actualDeadline = slaPolicy.calculateDeadline(priority, FIXED_NOW);

        // then
        assertThat(actualDeadline).isEqualTo(expectedDeadline);
    }
}
