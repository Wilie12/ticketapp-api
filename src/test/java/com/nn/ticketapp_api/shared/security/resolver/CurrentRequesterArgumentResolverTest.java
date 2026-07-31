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
    @DisplayName("Should extract UUID and assign INTERNAL access level for agent role")
    void shouldReturnInternalContextForAgent() {
        // given
        UUID expectedUUID = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        given(jwt.getSubject()).willReturn(expectedUUID.toString());

        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(jwt);

        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_AGENT"));
        given((Collection<GrantedAuthority>) authentication.getAuthorities()).willReturn(authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // then
        assertThat(result).isInstanceOf(RequesterContext.class);
        RequesterContext requesterContext = (RequesterContext) result;
        assertThat(requesterContext.userId()).isEqualTo(expectedUUID);
        assertThat(requesterContext.accessLevel()).isEqualTo(AccessLevel.INTERNAL);
        assertThat(requesterContext.isInternal()).isTrue();
    }

    @Test
    @DisplayName("Should extract UUID and assign STANDARD access level for regular user role")
    void shouldReturnStandardContextForUser() {
        UUID expectedUUID = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        given(jwt.getSubject()).willReturn(expectedUUID.toString());

        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(jwt);

        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        given((Collection<GrantedAuthority>) authentication.getAuthorities()).willReturn(authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

        // then
        assertThat(result).isInstanceOf(RequesterContext.class);
        RequesterContext requesterContext = (RequesterContext) result;
        assertThat(requesterContext.userId()).isEqualTo(expectedUUID);
        assertThat(requesterContext.accessLevel()).isEqualTo(AccessLevel.STANDARD);
        assertThat(requesterContext.isInternal()).isFalse();
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
        Jwt jwt = mock(Jwt.class);
        given(jwt.getSubject()).willReturn("");

        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(jwt);

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
}
