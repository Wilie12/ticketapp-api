package com.nn.ticketapp_api.shared.security.ratelimit;

import com.nn.ticketapp_api.shared.security.ratelimit.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class RateLimitingServiceTest {

    private static final long CAPACITY = 50L;
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1L);
    private static final Duration EXPIRATION_JITTER = Duration.ofMinutes(1L);

    @Mock
    private ProxyManager<String> proxyManager;
    @Mock
    private RemoteBucketBuilder<String> remoteBucketBuilder;
    @Captor
    private ArgumentCaptor<Supplier<BucketConfiguration>> configSupplierCaptor;

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties(CAPACITY, REFILL_DURATION, EXPIRATION_JITTER);
        rateLimitingService = new RateLimitingService(proxyManager, properties);
    }

    @Test
    @DisplayName("Should resolve distributed bucket with namespaced key and configure bandwidth limits")
    void shouldResolveBucketWithNamespacedKeyAndConfiguration() {
        // given
        String clientId = "user-123";
        String expectedNamespacedKey = "rate-limit:user-123";
        BucketProxy expectedBucket = mock(BucketProxy.class);

        given(proxyManager.builder()).willReturn(remoteBucketBuilder);
        given(remoteBucketBuilder.build(eq(expectedNamespacedKey), any(Supplier.class)))
                .willReturn(expectedBucket);

        // when
        Bucket actualBucket = rateLimitingService.resolveBucket(clientId);

        // then
        assertThat(actualBucket).isSameAs(expectedBucket);

        then(proxyManager).should().builder();
        then(remoteBucketBuilder).should().build(eq(expectedNamespacedKey), configSupplierCaptor.capture());

        BucketConfiguration capturedConfig = configSupplierCaptor.getValue().get();
        assertThat(capturedConfig.getBandwidths()).hasSize(1);

        Bandwidth bandwidth = capturedConfig.getBandwidths()[0];
        assertThat(bandwidth.getCapacity()).isEqualTo(50L);
        assertThat(bandwidth.getRefillPeriodNanos()).isEqualTo(Duration.ofMinutes(1).toNanos());
    }
}
