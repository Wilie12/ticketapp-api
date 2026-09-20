package com.nn.ticketapp_api.shared.security.ratelimit;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RateLimitingServiceTest {

    private final RateLimitingService rateLimitingService = new  RateLimitingService();

    @Test
    @DisplayName("Should create and reuse the same bucket for a give client key")
    void shouldCreateAndReuseBucket() {
        // given
        String clientId = "user-123";

        // when
        Bucket firstBucket = rateLimitingService.resolveBucket(clientId);
        Bucket secondBucket = rateLimitingService.resolveBucket(clientId);

        // then
        assertThat(firstBucket).isNotNull();
        assertThat(firstBucket).isSameAs(secondBucket);
    }

    @Test
    @DisplayName("Should enforce max 50 tokens per bucket capacity limit")
    void shouldEnforceTokenLimit() {
        // given
        Bucket bucket = rateLimitingService.resolveBucket("test-client");

        // when
        boolean canConsume50 = bucket.tryConsume(50);
        boolean canConsume51 = bucket.tryConsume(1);

        // then
        assertThat(canConsume50).isTrue();
        assertThat(canConsume51).isFalse();
    }
}
