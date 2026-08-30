package com.nn.ticketapp_api.communication.controller;

import com.nn.ticketapp_api.communication.api.request.CommunicationCreateRequest;
import com.nn.ticketapp_api.communication.api.response.CommunicationResponse;
import com.nn.ticketapp_api.communication.domain.CommunicationType;
import com.nn.ticketapp_api.communication.service.CommunicationService;
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

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommunicationController.class)
@Import({SecurityConfig.class, WebMvcConfig.class})
public class CommunicationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private CommunicationService communicationService;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private Clock clock;

    @Test
    @DisplayName("Should return 201 Create when USER adds a valid public comment")
    void shouldAddPublicCommentSuccessfully() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        CommunicationCreateRequest request = new CommunicationCreateRequest("Test public comment");

        CommunicationResponse mockResponse = new CommunicationResponse(
                UUID.randomUUID(),
                CommunicationType.PUBLIC_COMMENT,
                request.content(),
                authorId,
                Instant.now()
        );

        given(communicationService.addPublicComment(eq(ticketId), eq(authorId), eq(request.content())))
                .willReturn(mockResponse);

        // when
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", ticketId)
                        .with(jwt().jwt(builder -> builder.subject(authorId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PUBLIC_COMMENT"))
                .andExpect(jsonPath("$.content").value(request.content()))
                .andExpect(jsonPath("$.authorId").value(authorId.toString()));

        then(communicationService).should().addPublicComment(ticketId, authorId, request.content());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when comment content is missing or too short")
    void shouldRejectInvalidComment() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        CommunicationCreateRequest request = new CommunicationCreateRequest("x");

        // when
        mockMvc.perform(post("/api/v1/tickets/{id}/comments", ticketId)
                        .with(jwt().jwt(builder -> builder.subject(authorId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isBadRequest());

        then(communicationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should return 201 Created when AGENT adds a work note")
    void shouldAddWorkNoteSuccessfully() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        CommunicationCreateRequest request = new CommunicationCreateRequest("Test work note");

        CommunicationResponse mockResponse = new CommunicationResponse(
                UUID.randomUUID(),
                CommunicationType.WORK_NOTE,
                request.content(),
                agentId,
                Instant.now()
        );

        given(communicationService.addWorkNote(eq(ticketId), eq(agentId), eq(request.content())))
                .willReturn(mockResponse);

        // when
        mockMvc.perform(post("/api/v1/tickets/{id}/work-notes", ticketId)
                        .with(jwt().jwt(builder -> builder.subject(agentId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_AGENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("WORK_NOTE"))
                .andExpect(jsonPath("$.content").value(request.content()))
                .andExpect(jsonPath("$.authorId").value(agentId.toString()));

        then(communicationService).should().addWorkNote(ticketId, agentId, request.content());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when standard USER tries to add a work note")
    void shouldReturn403WhenUserTriesToAddWorkNote() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CommunicationCreateRequest request = new CommunicationCreateRequest("Test work note");

        // when
        mockMvc.perform(post("/api/v1/tickets/{id}/work-notes", ticketId)
                        .with(jwt().jwt(builder -> builder.subject(userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isForbidden());

        then(communicationService).shouldHaveNoInteractions();
    }
}
