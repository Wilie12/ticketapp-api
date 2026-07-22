package com.nn.ticketapp_api.shared.security.resolver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class CurrentUserIdArgumentResolverTest {

    private final CurrentUserIdArgumentResolver resolver = new CurrentUserIdArgumentResolver();

    private MethodParameter parameter;
    private NativeWebRequest webRequest;

    @BeforeEach
    void setUp() {
        parameter = mock(MethodParameter.class);
        webRequest = mock(NativeWebRequest.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully extract and pase UUID from JWT 'sub' claim")
    void shouldExtractUuidFromJwtSubject() throws Exception {
        // given
        UUID expectedUUID = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        given(jwt.getSubject()).willReturn(expectedUUID.toString());

        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Object result = resolver.resolveArgument(parameter, null, webRequest, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(UUID.class);
        assertThat(result).isEqualTo(expectedUUID);
    }

    @Test
    @DisplayName("Should throw AuthenticationCredentialsNotFoundException when user is completely unauthenticated")
    void shouldThrowExceptionWhenAuthenticationIsNull() {
        // when
        Throwable thrown = catchThrowable(() -> resolver.resolveArgument(parameter, null, webRequest, null));

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
        Throwable thrown = catchThrowable(() -> resolver.resolveArgument(parameter, null, webRequest, null));

        assertThat(thrown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT Token 'sub' claim is missing");
    }
}
