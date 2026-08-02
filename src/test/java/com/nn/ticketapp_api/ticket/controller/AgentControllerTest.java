package com.nn.ticketapp_api.ticket.controller;

import com.nn.ticketapp_api.shared.api.advice.GlobalExceptionHandler;
import com.nn.ticketapp_api.shared.api.response.PageResponse;
import com.nn.ticketapp_api.shared.config.WebMvcConfig;
import com.nn.ticketapp_api.shared.security.SecurityConfig;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    @MockitoBean
    private TicketService ticketService;
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
}
