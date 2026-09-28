package com.nn.ticketapp_api.shared.security.ratelimit;

import com.nn.ticketapp_api.shared.security.ratelimit.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private static final String KEY_PREFIX = "rate-limit:";

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties rateLimitProperties;

    public Bucket resolveBucket(String key) {
        String namespacedKey = KEY_PREFIX + key;
        return proxyManager.builder().build(namespacedKey, this::createBucketConfiguration);
    }

    private BucketConfiguration createBucketConfiguration() {
        log.debug("Initializing distributed rate limit bucket configuration");

        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimitProperties.capacity())
                .refillGreedy(rateLimitProperties.capacity(), rateLimitProperties.refillDuration())
                .build();

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }
}
