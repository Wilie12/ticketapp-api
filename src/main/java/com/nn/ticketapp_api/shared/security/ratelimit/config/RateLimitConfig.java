package com.nn.ticketapp_api.shared.security.ratelimit.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@RequiredArgsConstructor
public class RateLimitConfig {

    private static final RedisCodec<String, byte[]> BUCKET_CODEC =
            RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);

    private final RateLimitProperties rateLimitProperties;

    @Bean(destroyMethod = "close")
    public StatefulConnection<String, byte[]> bucket4jRedisConnection(RedisConnectionFactory redisConnectionFactory) {
        if (!(redisConnectionFactory instanceof LettuceConnectionFactory lettuceConnectionFactory)) {
            throw new IllegalStateException(
                    String.format(
                            "RateLimitConfig requires LettuceConnectionFactory, but found: %s",
                            redisConnectionFactory.getClass()
                    )
            );
        }

        AbstractRedisClient nativeClient = lettuceConnectionFactory.getRequiredNativeClient();

        return switch (nativeClient) {
            case RedisClient client -> client.connect(BUCKET_CODEC);
            case RedisClusterClient clusterClient -> clusterClient.connect(BUCKET_CODEC);
            default -> throw new IllegalStateException(
                    String.format("Unsupported Lettuce client type: %s", nativeClient.getClass())
            );
        };
    }

    @Bean
    public ProxyManager<String> proxyManager(StatefulConnection<String, byte[]> bucket4jRedisConnection) {
        ExpirationAfterWriteStrategy expirationStrategy = ExpirationAfterWriteStrategy
                .basedOnTimeForRefillingBucketUpToMax(rateLimitProperties.expirationJitter());

        var builder = switch (bucket4jRedisConnection) {
            case StatefulRedisConnection<String, byte[]> singleConnection ->
                    LettuceBasedProxyManager.builderFor(singleConnection);
            case StatefulRedisClusterConnection<String, byte[]> clusterConnection ->
                    LettuceBasedProxyManager.builderFor(clusterConnection);
            default -> throw new IllegalStateException(
                    String.format("Unsupported Lettuce connection type: %s", bucket4jRedisConnection.getClass())
            );
        };

        return builder
                .withExpirationStrategy(expirationStrategy)
                .build();
    }
}
