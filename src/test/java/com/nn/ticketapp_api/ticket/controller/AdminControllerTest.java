package com.nn.ticketapp_api.ticket.controller;

import com.nn.ticketapp_api.BaseControllerTest;
import com.nn.ticketapp_api.shared.api.response.PageResponse;
import com.nn.ticketapp_api.ticket.api.response.StatsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.service.AgentStatsService;
import com.nn.ticketapp_api.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.nn.ticketapp_api.shared.security.SecurityTestUtils.validJwt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
public class AdminControllerTest extends BaseControllerTest {

    @MockitoBean
    private AgentStatsService agentStatsService;
    @MockitoBean
    private TicketService ticketService;

    @Test
    @DisplayName("Should return global agent statistics array and 200 OK for ADMIN")
    void shouldReturnGlobalAgentStatsForAdmin() throws Exception {
        // given
        UUID agentId = UUID.randomUUID();

        StatsResponse mockResponse = new StatsResponse(agentId, 3L, 4L, 0L);

        given(agentStatsService.getAllAgentsStats()).willReturn(List.of(mockResponse));

        // when
        mockMvc.perform(get("/api/v1/admin/stats/agents")
                        .with(validJwt(UUID.randomUUID(), "ROLE_ADMIN")))
                // then
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

        // when
        mockMvc.perform(get("/api/v1/admin/stats/agents")
                        .with(validJwt(UUID.randomUUID(), "ROLE_AGENT")))
                // then
                .andExpect(status().isForbidden());

        then(agentStatsService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should return paginated list of all tickets and 200 OK for ADMIN")
    void shouldReturnAllTicketsForAdmin() throws Exception {
        // given
        UUID adminId = UUID.randomUUID();
        TicketResponse mockResponse = new TicketResponse(
                UUID.randomUUID(),
                "INC0000001",
                "Global issue overview",
                TicketStatus.NEW,
                Instant.parse("2026-09-24T17:00:00Z"),
                Instant.parse("2026-09-24T19:00:00Z")
        );

        PageResponse<TicketResponse> pageResponse = new PageResponse<>(
                List.of(mockResponse),
                0,
                20,
                1,
                1,
                true
        );

        given(ticketService.getAllTickets(any(Pageable.class))).willReturn(pageResponse);

        // when
        mockMvc.perform(get("/api/v1/admin/tickets")
                .with(validJwt(adminId, "ROLE_ADMIN")))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticketNumber").value("INC0000001"))
                .andExpect(jsonPath("$.totalElements").value(1));

        then(ticketService).should().getAllTickets(any(Pageable.class));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when AGENT attempts to access global ticket list")
    void shouldReturnForbiddenWhenAgentAccessesGlobalTickets() throws Exception {
        // when
        mockMvc.perform(get("/api/v1/admin/tickets")
                .with(validJwt(UUID.randomUUID(), "ROLE_AGENT")))
                // then
                .andExpect(status().isForbidden());

        then(ticketService).shouldHaveNoInteractions();
    }
}
