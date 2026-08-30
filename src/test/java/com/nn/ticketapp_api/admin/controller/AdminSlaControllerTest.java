package com.nn.ticketapp_api.admin.controller;

import com.nn.ticketapp_api.admin.api.request.SlaConfigurationUpdateRequest;
import com.nn.ticketapp_api.admin.api.response.SlaConfigurationResponse;
import com.nn.ticketapp_api.admin.service.SlaConfigurationService;
import com.nn.ticketapp_api.shared.config.WebMvcConfig;
import com.nn.ticketapp_api.shared.security.SecurityConfig;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.UUID;

import static com.nn.ticketapp_api.shared.security.SecurityTestUtils.validJwt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSlaController.class)
@Import({SecurityConfig.class, WebMvcConfig.class})
public class AdminSlaControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private SlaConfigurationService slaConfigurationService;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private Clock clock;

    @Test
    @DisplayName("Should allow ADMIN to update SLA configuration and return 200 OK")
    void shouldUpdateSlaConfigForAdmin() throws Exception {
        // given
        SlaConfigurationUpdateRequest request = new SlaConfigurationUpdateRequest(10);
        SlaConfigurationResponse response = new SlaConfigurationResponse(TicketPriority.HIGH, 10);

        given(slaConfigurationService.updateSlaConfiguration(eq(TicketPriority.HIGH), eq(10)))
                .willReturn(response);

        // when
        mockMvc.perform(put("/api/v1/admin/sla-configs/HIGH")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(validJwt(UUID.randomUUID(), "ROLE_ADMIN")))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.resolutionHours").value(10));

        then(slaConfigurationService).should().updateSlaConfiguration(TicketPriority.HIGH, 10);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when AGENT attempts to update SLA configuration")
    void shouldReturnForbiddenForAgent() throws Exception {
        // given
        SlaConfigurationUpdateRequest request = new SlaConfigurationUpdateRequest(5);

        // when
        mockMvc.perform(put("/api/v1/admin/sla-configs/HIGH")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(validJwt(UUID.randomUUID(), "ROLE_AGENT")))
                // then
                .andExpect(status().isForbidden());

        then(slaConfigurationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should trigger Shift-Left Validation and return 400 Bad Request on invalid input")
    void shouldReturnBadRequestOnInvalidInput() throws Exception {
        // given
        SlaConfigurationUpdateRequest request = new SlaConfigurationUpdateRequest(0);

        // when
        mockMvc.perform(put("/api/v1/admin/sla-configs/MEDIUM")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(validJwt(UUID.randomUUID(), "ROLE_ADMIN")))
                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.detail")
                        .value("Validation failed for field 'resolutionHours':" +
                                " Resolution hours must be at least 1 hour"));

        then(slaConfigurationService).shouldHaveNoInteractions();
    }
}
