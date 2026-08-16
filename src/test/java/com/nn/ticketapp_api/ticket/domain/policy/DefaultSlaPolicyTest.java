package com.nn.ticketapp_api.ticket.domain.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultSlaPolicyTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-03T10:00:00Z");
    private final SlaPolicy slaPolicy = new DefaultSlaPolicy();

    @Test
    @DisplayName("Should act as a pure function and add exact hours to creation time")
    void shouldCalculateDeadlineDeterministically() {
        // given
        Integer resolutionHours = 12;
        Instant expectedDeadline = FIXED_NOW.plus(12, ChronoUnit.HOURS);

        // when
        Instant actualDeadline = slaPolicy.calculateDeadline(FIXED_NOW, resolutionHours);

        // then
        assertThat(actualDeadline).isEqualTo(expectedDeadline);
    }
}
