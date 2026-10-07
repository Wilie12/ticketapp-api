package com.nn.ticketapp_api.ticket.audit;

import com.jayway.jsonpath.JsonPath;
import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.agent.domain.AgentProfile;
import com.nn.ticketapp_api.agent.repository.AgentProfileRepository;
import com.nn.ticketapp_api.team.domain.Team;
import com.nn.ticketapp_api.team.repository.TeamRepository;
import com.nn.ticketapp_api.ticket.api.request.TicketCreateRequest;
import com.nn.ticketapp_api.ticket.api.request.TicketPatchRequest;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static com.nn.ticketapp_api.shared.security.SecurityTestUtils.validJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TicketAuditIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private AgentProfileRepository agentProfileRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAllInBatch();
        agentProfileRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
        jdbcTemplate.execute("TRUNCATE TABLE revinfo CASCADE");
    }

    @Test
    @DisplayName("Should track complete audit trail including ADD and MOD revisions with distinct authors")
    void shouldTrackCompleteAuditTrailWithAuthors() throws Exception {
        // given
        Team team = Team.create("Security Operations", "SecOps");
        teamRepository.saveAndFlush(team);

        UUID creatorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        AgentProfile adminAgent = AgentProfile.create(adminId, team.getId());
        agentProfileRepository.saveAndFlush(adminAgent);

        TicketCreateRequest createRequest = new TicketCreateRequest(
                "Firewall rule block",
                "Cannot access core DB",
                TicketPriority.HIGH,
                team.getId()
        );

        // when
        MvcResult createResult = mockMvc.perform(post("/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .with(validJwt(creatorId, "ROLE_USER")))
                .andExpect(status().isCreated())
                .andReturn();

        String ticketIdStr = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");
        UUID ticketId = UUID.fromString(ticketIdStr);

        // when
        TicketPatchRequest patchRequest = new TicketPatchRequest(null, TicketPriority.CRITICAL, null);

        mockMvc.perform(patch("/api/v1/tickets/{id}", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest))
                .with(validJwt(adminId, "ROLE_ADMIN")))
                .andExpect(status().isOk());

        // then
        mockMvc.perform(get("/api/v1/tickets/{id}/history", ticketId)
                .with(validJwt(adminId, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].revisionType").value("ADD"))
                .andExpect(jsonPath("$[0].authorId").value(creatorId.toString()))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[1].revisionType").value("MOD"))
                .andExpect(jsonPath("$[1].authorId").value(adminId.toString()))
                .andExpect(jsonPath("$[1].priority").value("CRITICAL"));
    }
}
