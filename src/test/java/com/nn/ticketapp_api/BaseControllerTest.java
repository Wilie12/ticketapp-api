package com.nn.ticketapp_api;

import com.nn.ticketapp_api.shared.config.WebMvcConfig;
import com.nn.ticketapp_api.shared.security.SecurityConfig;
import com.nn.ticketapp_api.shared.security.ratelimit.RateLimitInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@Import({SecurityConfig.class, WebMvcConfig.class})
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected RateLimitInterceptor rateLimitInterceptor;
    @MockitoBean
    protected JwtDecoder jwtDecoder;
    @MockitoBean
    protected Clock clock;

    @BeforeEach
    void setupBase() throws Exception {
        given(rateLimitInterceptor.preHandle(any(), any(), any())).willReturn(true);
    }
}
