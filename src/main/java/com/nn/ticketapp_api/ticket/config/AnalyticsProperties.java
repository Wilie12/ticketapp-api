package com.nn.ticketapp_api.ticket.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.analytics")
public record AnalyticsProperties(
        @NotNull(message = "Refresh rate property cannot be null")
        @Min(value = 10000, message = "Refresh rate must be at least 10000ms (10 seconds) to prevent DB starvation")
        Long refreshRate
) {
}
