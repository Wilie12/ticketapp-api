package com.nn.ticketapp_api.ticket.controller;

import com.nn.ticketapp_api.agent.api.request.AgentStatusUpdateRequest;
import com.nn.ticketapp_api.agent.domain.AgentStatus;
import com.nn.ticketapp_api.agent.service.AgentProfileService;
import com.nn.ticketapp_api.shared.api.advice.GlobalExceptionHandler;
import com.nn.ticketapp_api.shared.api.response.PageResponse;
import com.nn.ticketapp_api.shared.config.WebMvcConfig;
import com.nn.ticketapp_api.shared.security.SecurityConfig;
import com.nn.ticketapp_api.ticket.api.response.StatsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.service.AgentStatsService;
import com.nn.ticketapp_api.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, WebMvcConfig.class})
public class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TicketService ticketService;
    @MockitoBean
    private AgentStatsService agentStatsService;
    @MockitoBean
    private AgentProfileService agentProfileService;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Should return paginated agent assigned tickets and 200 OK")
    void shouldReturnAgentTickets() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();

        TicketResponse mockResponse = new TicketResponse(
                UUID.randomUUID(),
                "INC0000001",
                "Title",
                TicketStatus.IN_PROGRESS,
                Instant.now(),
                null
        );
        PageResponse<TicketResponse> pageResponse = new PageResponse<>(
                List.of(mockResponse),
                0,
                20,
                1,
                1,
                true
        );

        given(ticketService.getAgentTickets(eq(agentId), any(Pageable.class))).willReturn(pageResponse);

        // when
        mockMvc.perform(get("/api/v1/agents/me/tickets")
                        .with(jwt().jwt(builder -> builder.subject(agentId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticketNumber").value("INC0000001"))
                .andExpect(jsonPath("$.totalElements").value(1));

        then(ticketService).should().getAgentTickets(eq(agentId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return agent statistics and 200 OK")
    void shouldReturnAgentStatistics() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();
        StatsResponse mockResponse = new StatsResponse(agentId, 5L, 10L, 2L);

        given(agentStatsService.getAgentStats(agentId)).willReturn(mockResponse);

        // when
        mockMvc.perform(get("/api/v1/agents/me/stats")
                        .with(jwt().jwt(builder -> builder.subject(agentId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(agentId.toString()))
                .andExpect(jsonPath("$.currentOpenTicketsCount").value(5))
                .andExpect(jsonPath("$.allResolvedTicketsCount").value(10))
                .andExpect(jsonPath("$.allSlaBreachedCount").value(2));

        then(agentStatsService).should().getAgentStats(agentId);
    }

    @Test
    @DisplayName("Should successfully update agent status and return 204 No Content")
    void shouldUpdateAgentStatusSuccessfully() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();
        AgentStatusUpdateRequest request = new AgentStatusUpdateRequest(AgentStatus.AVAILABLE);

        // when
        mockMvc.perform(put("/api/v1/agents/me/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt(builder -> builder.subject(agentId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                // then
                .andExpect(status().isNoContent());

        then(agentProfileService).should().updateStatus(eq(agentId), eq(AgentStatus.AVAILABLE));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when status payload is empty")
    void shouldReturnBadRequestWhenStatusIsNull() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();
        String invalidJson = "{}";

        // when
        mockMvc.perform(put("/api/v1/agents/me/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
                .with(jwt().jwt(builder -> builder.subject(agentId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed for field 'status': Status cannot be null"));

        then(agentProfileService).shouldHaveNoInteractions();
    }
}
