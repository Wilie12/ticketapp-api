package com.nn.ticketapp_api.shared.security.ratelimit.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @Min(value = 1, message = "Bucket capacity must be at least 1")
        long capacity,
        @NotNull(message = "Refill duration must not be null")
        Duration refillDuration,
        @NotNull(message = "Expiration jitter duration must not be null")
        Duration expirationJitter
) {
}
