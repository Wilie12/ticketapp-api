package com.nn.ticketapp_api.shared.security.ratelimit;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public boolean preHandle(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull Object handler
    ) throws Exception {
        String clientId = resolveClientId(request);
        Bucket bucket = rateLimitingService.resolveBucket(clientId);

        if (bucket.tryConsume(1)) {
            return true;
        }

        log.warn("Rate limit exceeded (HTTP 429) for client: {}", clientId);
        rejectWithProblemDetail(response, request.getRequestURI());

        return false;
    }

    private String resolveClientId(HttpServletRequest request) {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast)
                .map(Jwt::getSubject)
                .orElseGet(() -> getClientIp(request));
    }

    private String getClientIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .filter(header -> !header.isBlank())
                .map(header -> header.split(",")[0])
                .orElseGet(request::getRemoteAddr);
    }

    private void rejectWithProblemDetail(HttpServletResponse response, String instanceUri) throws Exception {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/problem+json");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "API rate limit exceeded. Please try again later."
        );
        problemDetail.setTitle("Too Many Requests");
        problemDetail.setProperty("timestamp", Instant.now(clock));
        problemDetail.setInstance(URI.create(instanceUri));

        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }
}
