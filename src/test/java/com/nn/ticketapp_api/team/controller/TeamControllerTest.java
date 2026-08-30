package com.nn.ticketapp_api.team.controller;

import com.nn.ticketapp_api.shared.config.WebMvcConfig;
import com.nn.ticketapp_api.shared.security.SecurityConfig;
import com.nn.ticketapp_api.team.api.advice.TeamExceptionHandler;
import com.nn.ticketapp_api.team.api.request.TeamCreateRequest;
import com.nn.ticketapp_api.team.api.request.TeamUpdateRequest;
import com.nn.ticketapp_api.team.api.response.TeamResponse;
import com.nn.ticketapp_api.team.exception.TeamAlreadyExistsException;
import com.nn.ticketapp_api.team.exception.TeamNotFoundException;
import com.nn.ticketapp_api.team.service.TeamService;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@Import({SecurityConfig.class, WebMvcConfig.class})
public class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TeamService teamService;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private Clock clock;

    @Test
    @DisplayName("Should create team and return 201 Created")
    void shouldCreateTeamSuccessfully() throws Exception {
        // given
        TeamCreateRequest request = new TeamCreateRequest("Network", "Network support");
        TeamResponse response = new TeamResponse(
                UUID.randomUUID(),
                "Network",
                "Network support",
                null,
                null
        );

        given(teamService.createTeam(any(TeamCreateRequest.class))).willReturn(response);

        // when
        mockMvc.perform(post("/api/v1/teams")
                        .with(validJwt("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Network"))
                .andExpect(jsonPath("$.description").value("Network support"));

        then(teamService).should().createTeam(request);
    }

    @Test
    @DisplayName("Should return 403 Forbidden without ADMIN role")
    void shouldReturn403WhenCreatingTeamWithoutAdminRole() throws Exception {
        // given
        TeamCreateRequest request = new TeamCreateRequest("Network", "Description");

        // when
        mockMvc.perform(post("/api/v1/teams")
                        .with(validJwt("ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isForbidden());

        then(teamService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should return 400 Bad Request on validation failure")
    void shouldFailValidationWhenCreatingTeam() throws Exception {
        // given
        UUID adminId = UUID.randomUUID();
        TeamCreateRequest invalidRequest = new TeamCreateRequest("", "Description");

        // when
        mockMvc.perform(post("/api/v1/teams")
                        .with(validJwt("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        then(teamService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should fetch team and return 200 OK")
    void shouldFetchTeamSuccessfully() throws Exception {
        // given
        UUID teamId = UUID.randomUUID();
        TeamResponse response = new TeamResponse(
                teamId,
                "Hardware",
                "Hardware support",
                null,
                null
        );

        given(teamService.getTeam(teamId)).willReturn(response);

        // when
        mockMvc.perform(get("/api/v1/teams/{id}", teamId)
                        .with(validJwt("ROLE_AGENT")))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hardware"))
                .andExpect(jsonPath("$.description").value("Hardware support"));

        then(teamService).should().getTeam(teamId);
    }

    @Test
    @DisplayName("Should return 404 Not Found if team is missing")
    void shouldReturn404WhenTeamNotFound() throws Exception {
        // given
        UUID teamId = UUID.randomUUID();
        given(teamService.getTeam(teamId)).willThrow(new TeamNotFoundException(teamId));

        // when
        mockMvc.perform(get("/api/v1/teams/{id}", teamId)
                        .with(validJwt("ROLE_USER")))
                // then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Team Not Found"))
                .andExpect(jsonPath("$.detail")
                        .value(String.format("Team with ID %s not found", teamId)));

        then(teamService).should().getTeam(teamId);
    }

    @Test
    @DisplayName("Should fetch all teams and return 200 OK")
    void shouldGetAllTeamsSuccessfully() throws Exception {
        // given
        TeamResponse response1 = new TeamResponse(
                UUID.randomUUID(),
                "Network",
                "Desc",
                null,
                null
        );
        TeamResponse response2 = new TeamResponse(
                UUID.randomUUID(),
                "System",
                "Desc",
                null,
                null
        );

        given(teamService.getAllTeams()).willReturn(List.of(response1, response2));

        // when
        mockMvc.perform(get("/api/v1/teams")
                        .with(validJwt("ROLE_USER")))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Network"))
                .andExpect(jsonPath("$[1].name").value("System"));

        then(teamService).should().getAllTeams();
    }

    @Test
    @DisplayName("Should update team and return 200 OK")
    void shouldUpdateTeamSuccessfully() throws Exception {
        // given
        UUID teamId = UUID.randomUUID();
        TeamUpdateRequest request = new TeamUpdateRequest("Updated Network", null);
        TeamResponse response = new TeamResponse(
                teamId,
                "Updated Network",
                "Old description",
                null,
                null
        );

        given(teamService.updateTeam(teamId, request)).willReturn(response);

        // when
        mockMvc.perform(patch("/api/v1/teams/{id}", teamId)
                        .with(validJwt("ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Network"));

        then(teamService).should().updateTeam(teamId, request);
    }

    @Test
    @DisplayName("Should return 409 Conflict on duplicate name")
    void shouldReturn409OnUpdateConflict() throws Exception {
        // given
        UUID teamId = UUID.randomUUID();
        TeamUpdateRequest request = new TeamUpdateRequest("Existing name", null);

        given(teamService.updateTeam(eq(teamId), any(TeamUpdateRequest.class)))
                .willThrow(new TeamAlreadyExistsException(("Existing name")));

        // when
        mockMvc.perform(patch("/api/v1/teams/{id}", teamId)
                .with(validJwt("ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Team Already Exists"));

        then(teamService).should().updateTeam(teamId, request);
    }

    private RequestPostProcessor validJwt(String role) {
        return jwt()
                .jwt(builder -> builder.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority(role));
    }
}
