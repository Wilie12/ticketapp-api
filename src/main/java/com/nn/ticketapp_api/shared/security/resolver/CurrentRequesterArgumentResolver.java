package com.nn.ticketapp_api.shared.security.resolver;

import com.nn.ticketapp_api.shared.security.annotation.CurrentRequester;
import com.nn.ticketapp_api.shared.security.domain.AccessLevel;
import com.nn.ticketapp_api.shared.security.domain.RequesterContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public class CurrentRequesterArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String ROLE_AGENT = "ROLE_AGENT";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentRequester.class)
                && parameter.getParameterType().equals(RequesterContext.class);
    }

    @Override
    public @Nullable Object resolveArgument(
            @NonNull MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationCredentialsNotFoundException("User is not authenticated via valid JWT");
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("JWT Token 'sub' claim is missing");
        }

        UUID userId = UUID.fromString(subject);

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        boolean isInternalUser = authorities.stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(role -> role.equals(ROLE_AGENT) || role.equals(ROLE_ADMIN));

        AccessLevel accessLevel = isInternalUser ? AccessLevel.INTERNAL : AccessLevel.STANDARD;

        return new RequesterContext(userId, accessLevel);
    }
}
