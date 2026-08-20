package com.nn.ticketapp_api.agent.controller;

import com.nn.ticketapp_api.agent.api.request.AgentProfileCreateRequest;
import com.nn.ticketapp_api.agent.api.response.AgentProfileResponse;
import com.nn.ticketapp_api.agent.domain.AgentProfile;
import com.nn.ticketapp_api.agent.domain.AgentStatus;
import com.nn.ticketapp_api.agent.exception.AgentProfileAlreadyExistsException;
import com.nn.ticketapp_api.agent.service.AgentProfileService;
import com.nn.ticketapp_api.shared.api.advice.GlobalExceptionHandler;
import com.nn.ticketapp_api.shared.config.WebMvcConfig;
import com.nn.ticketapp_api.shared.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentProfileController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, WebMvcConfig.class})
public class AgentProfileControllerTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-01T12:00:00Z");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private AgentProfileService agentProfileService;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Should successfully provision agent profile and return 201 Created for ADMIN")
    void shouldProvisionAgentProfileForAdmin() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        AgentProfileCreateRequest request = new AgentProfileCreateRequest(agentId, teamId);
        AgentProfileResponse expectedResponse = new AgentProfileResponse(
                agentId,
                teamId,
                AgentStatus.OFFLINE,
                FIXED_NOW
        );

        given(agentProfileService.createAgentProfile(agentId, teamId)).willReturn(expectedResponse);

        // when
        mockMvc.perform(post("/api/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt(builder -> builder.subject(adminId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agentId").value(agentId.toString()))
                .andExpect(jsonPath("$.teamId").value(teamId.toString()))
                .andExpect(jsonPath("$.status").value("OFFLINE"));

        then(agentProfileService).should().createAgentProfile(agentId, teamId);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when validation fails")
    void shouldReturnBadRequestOnValidationFailure() throws Exception {
        // given
        UUID adminId = UUID.randomUUID();
        String invalidJson = "{}";

        // when
        mockMvc.perform(post("/api/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
                .with(jwt().jwt(builder -> builder.subject(adminId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());

        then(agentProfileService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should return 409 Conflict when attempting to provision duplicate agent")
    void shouldReturnConflictOnDuplicateAgent() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        AgentProfileCreateRequest request = new AgentProfileCreateRequest(agentId, teamId);

        given(agentProfileService.createAgentProfile(agentId, teamId))
                .willThrow(new AgentProfileAlreadyExistsException(agentId));

        // when
        mockMvc.perform(post("/api/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt(builder -> builder.subject(adminId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                // then
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value(String.format("Agent profile with ID %s already exists", agentId)));

        then(agentProfileService).should().createAgentProfile(agentId, teamId);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when AGENT attempts to provision agent profile")
    void shouldReturnForbiddenForAgentRole() throws Exception {
        // given
        UUID callerAgentId = UUID.randomUUID();
        AgentProfileCreateRequest request = new AgentProfileCreateRequest(UUID.randomUUID(), UUID.randomUUID());

        // when
        mockMvc.perform(post("/api/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(jwt().jwt(builder -> builder.subject(callerAgentId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                // then
                .andExpect(status().isForbidden());

        then(agentProfileService).shouldHaveNoInteractions();
    }
}
