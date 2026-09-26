package com.nn.ticketapp_api.shared.security;

import com.nn.ticketapp_api.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityConfigTest extends BaseIntegrationTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    @DisplayName("Should successfully load application context and instantiate SecurityFilterChain bean")
    void contextLoadsAndSecurityFilterChainIsConfigured() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    @DisplayName("Should permit unauthenticated access to whitelisted actuator health and prometheus endpoints")
    void shouldPermitAccessToWhitelistedActuatorEndpoints() throws Exception {
        // when
        mockMvc.perform(get("/actuator/health"))
                // then
                .andExpect(status().isOk());

        // when
        mockMvc.perform(get("/actuator/prometheus"))
                // then
                .andExpect(status().isOk());
    }
}
