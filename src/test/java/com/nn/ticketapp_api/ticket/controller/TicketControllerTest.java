package com.nn.ticketapp_api.ticket.controller;

import com.nn.ticketapp_api.shared.api.response.PageResponse;
import com.nn.ticketapp_api.shared.config.WebMvcConfig;
import com.nn.ticketapp_api.shared.security.SecurityConfig;
import com.nn.ticketapp_api.shared.security.domain.RequesterContext;
import com.nn.ticketapp_api.ticket.api.request.ResolutionRequest;
import com.nn.ticketapp_api.ticket.api.request.TicketCreateRequest;
import com.nn.ticketapp_api.ticket.api.request.TicketPatchRequest;
import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.exception.InvalidStatusTransitionException;
import com.nn.ticketapp_api.ticket.exception.TicketClosedException;
import com.nn.ticketapp_api.ticket.exception.TicketNotFoundException;
import com.nn.ticketapp_api.ticket.exception.TicketOwnershipException;
import com.nn.ticketapp_api.ticket.facade.TicketFacade;
import com.nn.ticketapp_api.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.nn.ticketapp_api.shared.security.SecurityTestUtils.validJwt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, WebMvcConfig.class})
public class TicketControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TicketService ticketService;
    @MockitoBean
    private TicketFacade ticketFacade;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private Clock clock;

    @Test
    @DisplayName("Should successfully create ticket and return 201 Created")
    void shouldCreateTicket() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        TicketCreateRequest request = new TicketCreateRequest(
                "Test ticket",
                "Desc",
                TicketPriority.LOW,
                teamId
        );

        TicketResponse mockResponse = new TicketResponse(
                UUID.randomUUID(),
                "INC0000001",
                request.title(),
                TicketStatus.NEW,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        given(ticketService.createTicket(any(TicketCreateRequest.class), eq(userId)))
                .willReturn(mockResponse);

        // when
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(validJwt(userId, "ROLE_USER")))
                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNumber").value("INC0000001"))
                .andExpect(jsonPath("$.title").value("Test ticket"))
                .andExpect(jsonPath("$.status").value("NEW"));

        then(ticketService).should().createTicket(any(TicketCreateRequest.class), eq(userId));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        // given
        TicketCreateRequest invalidRequest = new TicketCreateRequest(
                "Bad",
                "Desc",
                TicketPriority.LOW,
                UUID.randomUUID()
        );

        // when
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(validJwt(UUID.randomUUID(), "ROLE_USER")))
                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail")
                        .value("Validation failed for field 'title':" +
                                " Title must be at least 5 characters long")
                );
    }

    @Test
    @DisplayName("Should return tickets created by user")
    void shouldReturnTicketsCreatedByUser() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        TicketResponse mockTicket = new TicketResponse(
                UUID.randomUUID(),
                "INC0000001",
                "Test ticket",
                TicketStatus.NEW,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        given(ticketService.getUserTickets(userId)).willReturn(List.of(mockTicket));

        // when
        mockMvc.perform(get("/api/v1/tickets")
                        .with(validJwt(userId, "ROLE_USER")))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].ticketNumber").value("INC0000001"))
                .andExpect(jsonPath("$[0].title").value("Test ticket"))
                .andExpect(jsonPath("$[0].status").value("NEW"));

        then(ticketService).should().getUserTickets(eq(userId));
    }

    @Test
    @DisplayName("Should return ticket details and 200 OK when user owns the ticket")
    void shouldReturnTicketDetails() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();

        TicketDetailsResponse mockResponse = new TicketDetailsResponse(
                ticketId,
                "INC0000001",
                "Test ticket",
                "Desc",
                TicketStatus.NEW,
                null,
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        given(ticketFacade.getTicketDetails(eq(ticketId), any(RequesterContext.class))).willReturn(mockResponse);

        // when
        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .with(validJwt(UUID.randomUUID(), "ROLE_USER")))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.ticketNumber").value("INC0000001"))
                .andExpect(jsonPath("$.title").value("Test ticket"));

        then(ticketFacade).should().getTicketDetails(eq(ticketId), any(RequesterContext.class));
    }

    @Test
    @DisplayName("Should return 404 Not Found when ticket does not exist")
    void shouldReturn404WhenTicketNotFound() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();

        given(ticketFacade.getTicketDetails(eq(ticketId), any(RequesterContext.class)))
                .willThrow(new TicketNotFoundException(ticketId));

        // when
        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .with(validJwt(UUID.randomUUID(), "ROLE_USER")))
                // then
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail")
                        .value(String.format("Ticket with ID %s not found", ticketId)));

        then(ticketFacade).should().getTicketDetails(eq(ticketId), any(RequesterContext.class));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when user tries to access someone else's ticket")
    void shouldReturn403WhenUserIsNotOwner() throws Exception {
        // given
        UUID fakeRequesterId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        given(ticketFacade.getTicketDetails(eq(ticketId), any(RequesterContext.class)))
                .willThrow(new TicketOwnershipException(ticketId, fakeRequesterId));

        // when
        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .with(validJwt(fakeRequesterId, "ROLE_USER")))
                // then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail")
                        .value(String.format("User %s is not the owner of ticket %s", fakeRequesterId, ticketId)));

        then(ticketFacade).should().getTicketDetails(eq(ticketId), any(RequesterContext.class));
    }

    @Test
    @DisplayName("Should successfully assign ticket to agent and return 200 OK")
    void shouldAssignTicket() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        TicketResponse mockResponse = new TicketResponse(
                ticketId,
                "INC0000001",
                "Test title",
                TicketStatus.IN_PROGRESS,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        given(ticketService.assignTicket(ticketId, agentId)).willReturn(mockResponse);

        // when
        mockMvc.perform(post("/api/v1/tickets/{id}/assign", ticketId)
                        .with(validJwt(agentId, "ROLE_AGENT")))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        then(ticketService).should().assignTicket(eq(ticketId), eq(agentId));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when standard USER tries to assign a ticket")
    void shouldReturn403WhenUserTriesToAssignATicket() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();

        // when
        mockMvc.perform(post("/api/v1/tickets/{id}/assign", ticketId)
                        .with(validJwt(UUID.randomUUID(), "ROLE_USER")))
                // then
                .andExpect(status().isForbidden());

        then(ticketService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should successfully resolve ticket and return 200 OK")
    void shouldResolveTicket() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        ResolutionRequest request = new ResolutionRequest("Provided resolution.");

        TicketResponse mockResponse = new TicketResponse(
                ticketId,
                "INC0000001",
                "Test title",
                TicketStatus.RESOLVED,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        given(ticketService.resolveTicket(ticketId, agentId, request.resolutionNote())).willReturn(mockResponse);

        // when
        mockMvc.perform(post("/api/v1/tickets/{id}/resolve", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(validJwt(agentId, "ROLE_AGENT")))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        then(ticketService).should().resolveTicket(eq(ticketId), eq(agentId), eq(request.resolutionNote()));
    }

    @Test
    @DisplayName("Should return 409 Conflict when trying to resolve a closed ticket")
    void shouldReturn409OnInvalidStateTransition() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        ResolutionRequest request = new ResolutionRequest("Provided resolution.");

        given(ticketService.resolveTicket(ticketId, agentId, request.resolutionNote()))
                .willThrow(new InvalidStatusTransitionException("Ticket is already closed and cannot be resolved"));

        // when
        mockMvc.perform(post("/api/v1/tickets/{id}/resolve", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(validJwt(agentId, "ROLE_AGENT")))
                // then
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail")
                        .value("Ticket is already closed and cannot be resolved"));
    }

    @Test
    @DisplayName("Should return 409 Conflict when attempting to modify a closed ticket")
    void shouldReturn409WhenTicketIsClosed() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        TicketPatchRequest request = new TicketPatchRequest(
                "New title",
                TicketPriority.HIGH,
                UUID.randomUUID()
        );

        given(ticketService.updateTicketDetails(ticketId, request, agentId))
                .willThrow(new TicketClosedException("Ticket is closed and cannot be modified"));

        // when
        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(validJwt(agentId, "ROLE_AGENT")))
                // then
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail")
                        .value("Ticket is closed and cannot be modified"));

        then(ticketService).should().updateTicketDetails(eq(ticketId), eq(request), eq(agentId));
    }

    @Test
    @DisplayName("Should successfully reopen ticket and return 200 OK")
    void shouldReopenTicket() throws Exception {
        // given
        UUID requesterId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        TicketResponse mockResponse = new TicketResponse(
                ticketId,
                "INC0000001",
                "Reopened ticket",
                TicketStatus.IN_PROGRESS,
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        given(ticketService.reopenTicket(ticketId, requesterId)).willReturn(mockResponse);

        // when
        mockMvc.perform(post("/api/v1/tickets/{id}/reopen", ticketId)
                        .with(validJwt(requesterId, "ROLE_USER")))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.title").value("Reopened ticket"));

        then(ticketService).should().reopenTicket(eq(ticketId), eq(requesterId));
    }

    @Test
    @DisplayName("Should successfully update ticket details and return 200 OK")
    void shouldUpdateTicketDetails() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID newTeamId = UUID.randomUUID();

        TicketPatchRequest request = new TicketPatchRequest(
                "New title",
                TicketPriority.CRITICAL,
                newTeamId
        );

        TicketResponse mockResponse = new TicketResponse(
                ticketId,
                "INC0000001",
                "New title",
                TicketStatus.IN_PROGRESS,
                Instant.now(),
                Instant.now().plusSeconds(1800)
        );

        given(ticketService.updateTicketDetails(ticketId, request, agentId)).willReturn(mockResponse);

        // when
        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(validJwt(agentId, "ROLE_AGENT")))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"));

        then(ticketService).should().updateTicketDetails(eq(ticketId), eq(request), eq(agentId));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when standard USER tries to modify a ticket via PATCH")
    void shouldReturn403WhenUserTriesToPatchTicket() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();

        TicketPatchRequest request = new TicketPatchRequest(
                "New title",
                TicketPriority.MEDIUM,
                UUID.randomUUID()
        );

        // when
        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(validJwt(UUID.randomUUID(), "ROLE_USER")))
                // then
                .andExpect(status().isForbidden());

        then(ticketService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should return paginated unassigned tickets queue and 200 OK")
    void shouldReturnUnassignedQueue() throws Exception {
        // given
        UUID teamId = UUID.randomUUID();

        TicketResponse mockResponse = new TicketResponse(
                UUID.randomUUID(),
                "INC0000001",
                "Title",
                TicketStatus.NEW,
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

        given(ticketService.getUnassignedQueue(eq(teamId), any(Pageable.class))).willReturn(pageResponse);

        // when
        mockMvc.perform(get("/api/v1/tickets/queue")
                .param("teamId", teamId.toString())
                .param("page", "0")
                .param("size", "20")
                .with(validJwt(UUID.randomUUID(), "ROLE_AGENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticketNumber").value("INC0000001"))
                .andExpect(jsonPath("$.totalElements").value(1));

        then(ticketService).should().getUnassignedQueue(eq(teamId), any(Pageable.class));
    }
}