package com.nn.ticketapp_api.shared.security;

import com.nn.ticketapp_api.shared.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SecurityConfig.class)
public class SecurityConfigTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;
    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    @DisplayName("Should successfully load application context and instantiate SecurityFilterChain bean")
    void contextLoadsAndSecurityFilterChainIsConfigured() {
        assertThat(securityFilterChain).isNotNull();
    }
}
