package com.nn.ticketapp_api.ticket.controller;

import com.nn.ticketapp_api.shared.api.advice.GlobalExceptionHandler;
import com.nn.ticketapp_api.shared.config.WebMvcConfig;
import com.nn.ticketapp_api.shared.security.SecurityConfig;
import com.nn.ticketapp_api.ticket.api.response.StatsResponse;
import com.nn.ticketapp_api.ticket.service.AgentStatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, WebMvcConfig.class})
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AgentStatsService agentStatsService;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private Clock clock;

    @Test
    @DisplayName("Should return global agent statistics array and 200 OK for ADMIN")
    void shouldReturnGlobalAgentStatsForAdmin() throws Exception {
        // given
        UUID adminId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        StatsResponse mockResponse = new StatsResponse(agentId, 3L, 4L, 0L);

        given(agentStatsService.getAllAgentsStats()).willReturn(List.of(mockResponse));

        // when
        mockMvc.perform(get("/api/v1/admin/stats/agents")
                .with(jwt().jwt(builder -> builder.subject(adminId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].agentId").value(agentId.toString()))
                .andExpect(jsonPath("$[0].currentOpenTicketsCount").value(3))
                .andExpect(jsonPath("$[0].allResolvedTicketsCount").value(4))
                .andExpect(jsonPath("$[0].allSlaBreachedCount").value(0));

        then(agentStatsService).should().getAllAgentsStats();
    }

    @Test
    @DisplayName("Should return 403 Forbidden when AGENT attempts to access global statistics")
    void shouldReturnForbiddenForAgent() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();

        // when
        mockMvc.perform(get("/api/v1/admin/stats/agents")
                .with(jwt().jwt(builder -> builder.subject(agentId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                .andExpect(status().isForbidden());

        then(agentStatsService).shouldHaveNoInteractions();
    }
}
