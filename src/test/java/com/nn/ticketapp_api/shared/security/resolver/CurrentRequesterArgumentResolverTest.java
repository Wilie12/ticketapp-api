package com.nn.ticketapp_api.shared.security.resolver;

import com.nn.ticketapp_api.shared.security.domain.AccessLevel;
import com.nn.ticketapp_api.shared.security.domain.RequesterContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class CurrentRequesterArgumentResolverTest {

    private final CurrentRequesterArgumentResolver resolver = new CurrentRequesterArgumentResolver();

    private MethodParameter methodParameter;
    private NativeWebRequest webRequest;

    @BeforeEach
    void setUp() {
        methodParameter = mock(MethodParameter.class);
        webRequest = mock(NativeWebRequest.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should extract UUID and assign ADMIN access level for admin role")
    void shouldReturnAdminContextForAdmin() {
        // given
        UUID adminId = UUID.randomUUID();
        Authentication authentication = mockAuthentication(adminId.toString(), "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // then
        assertThat(result).isInstanceOf(RequesterContext.class);
        RequesterContext context = (RequesterContext) result;
        assertThat(context.userId()).isEqualTo(adminId);
        assertThat(context.accessLevel()).isEqualTo(AccessLevel.ADMIN);
        assertThat(context.isInternal()).isTrue();
        assertThat(context.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("Should extract UUID and assign AGENT access level for agent role")
    void shouldReturnInternalContextForAgent() {
        // given
        UUID agentId = UUID.randomUUID();
        Authentication authentication = mockAuthentication(agentId.toString(), "ROLE_AGENT");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // then
        assertThat(result).isInstanceOf(RequesterContext.class);
        RequesterContext context = (RequesterContext) result;
        assertThat(context.userId()).isEqualTo(agentId);
        assertThat(context.accessLevel()).isEqualTo(AccessLevel.AGENT);
        assertThat(context.isInternal()).isTrue();
        assertThat(context.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("Should extract UUID and assign STANDARD access level for regular user role")
    void shouldReturnStandardContextForUser() {
        UUID userId = UUID.randomUUID();
        Authentication authentication = mockAuthentication(userId.toString(), "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // then
        assertThat(result).isInstanceOf(RequesterContext.class);
        RequesterContext context = (RequesterContext) result;
        assertThat(context.userId()).isEqualTo(userId);
        assertThat(context.accessLevel()).isEqualTo(AccessLevel.STANDARD);
        assertThat(context.isInternal()).isFalse();
        assertThat(context.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("Should throw AuthenticationCredentialsNotFoundException when completely unauthenticated")
    void shouldThrowExceptionWhenAuthenticationIsNull() {
        // when
        Throwable thrown = catchThrowable(
                () -> resolver.resolveArgument(methodParameter, null, webRequest, null)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessageContaining("User is not authenticated via valid JWT");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when valid JWT is provided but 'sub' claim is empty")
    void shouldThrowExceptionWhenSubjectIsMissing() {
        // given
        Authentication authentication = mockAuthentication("", "ROLE_AGENT");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Throwable thrown = catchThrowable(
                () -> resolver.resolveArgument(methodParameter, null, webRequest, null)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT Token 'sub' claim is missing");
    }

    private Authentication mockAuthentication(String subject, String role) {
        Jwt jwt = mock(Jwt.class);
        given(jwt.getSubject()).willReturn(subject);

        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(jwt);

        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        given((Collection<GrantedAuthority>) authentication.getAuthorities()).willReturn(authorities);

        return authentication;
    }
}
