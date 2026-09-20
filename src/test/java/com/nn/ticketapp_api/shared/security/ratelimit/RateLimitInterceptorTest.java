package com.nn.ticketapp_api.shared.security.ratelimit;

import com.nn.ticketapp_api.shared.config.WebMvcConfig;
import com.nn.ticketapp_api.shared.security.SecurityConfig;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.UUID;

import static com.nn.ticketapp_api.shared.security.SecurityTestUtils.validJwt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RateLimitTestController.class)
@Import({SecurityConfig.class, WebMvcConfig.class, RateLimitInterceptor.class})
public class RateLimitInterceptorTest {

    // TODO - naprawić i stworzyć BaseControllerTest do importów z SSOT, znaleźć problem
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateLimitingService rateLimitingService;
    @MockitoBean
    private Clock clock;

    @Test
    @DisplayName("Should process request when bucket has available tokens")
    void shouldProcessRequestWhenTokensAvailable() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Bucket mockBucket = mock(Bucket.class);

        given(rateLimitingService.resolveBucket(userId.toString())).willReturn(mockBucket);
        given(mockBucket.tryConsume(1)).willReturn(true);

        // when
        mockMvc.perform(get("/api/v1/test-rate-limit")
                .with(validJwt(userId, "ROLE_USER")))
                // then
                .andExpect(status().isOk());

        then(rateLimitingService).should().resolveBucket(userId.toString());
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests and RFC 7808 payload when tokens are depleted")
    void shouldRejectRequestWhenTokensDepleted() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Bucket mockBucket = mock(Bucket.class);

        given(rateLimitingService.resolveBucket(userId.toString())).willReturn(mockBucket);
        given(mockBucket.tryConsume(1)).willReturn(false);

        // when
        mockMvc.perform(get("/api/v1/test-rate-limit")
                .with(validJwt(userId, "ROLE_USER")))
                // then
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.detail")
                        .value("API rate limit exceeded. Please try again later."))
                .andExpect(jsonPath("$.instance").value("/api/v1/test-rate-limit"));

        then(rateLimitingService).should().resolveBucket(userId.toString());
    }
}
