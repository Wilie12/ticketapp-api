package com.nn.ticketapp_api.ticket.listener;

import com.nn.ticketapp_api.agent.domain.event.AgentAvailableEvent;
import com.nn.ticketapp_api.agent.service.AgentProfileService;
import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import com.nn.ticketapp_api.ticket.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class TicketAssignmentOrchestratorTest {

    @Mock
    private TicketService ticketService;
    @Mock
    private AgentProfileService agentProfileService;
    @InjectMocks
    private TicketAssignmentOrchestrator orchestrator;

    @Test
    @DisplayName("Should evaluate capacity for specific agent when AgentAvailableEvent is received")
    void shouldEvaluateCapacityOnAgentAvailable() {
        // given
        UUID agentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        AgentAvailableEvent event = new AgentAvailableEvent(agentId, teamId);

        // when
        orchestrator.onAgentAvailable(event);

        // then
        then(ticketService).should().evaluateAndFillAgentCapacity(agentId, teamId);
    }

    @Test
    @DisplayName("Should evaluate capacity for specific agent when TicketResolvedEvent is received")
    void shouldEvaluateCapacityOnTicketResolved() {
        // given
        UUID agentId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        TicketResolvedEvent event = new TicketResolvedEvent(ticketId, agentId, teamId, "Resolution note");

        // when
        orchestrator.onTicketResolved(event);

        // then
        then(ticketService).should().evaluateAndFillAgentCapacity(agentId, teamId);
    }

    @Test
    @DisplayName("Should evaluate capacity for all available agents in team when TicketCreatedEvent is received")
    void shouldEvaluateCapacityForAllAvailableAgentsOnTicketCreated() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        TicketCreatedEvent event = new TicketCreatedEvent(ticketId, teamId);

        UUID firstAgentId = UUID.randomUUID();
        UUID secondAgentId = UUID.randomUUID();
        List<UUID> availableAgents = List.of(firstAgentId, secondAgentId);

        given(agentProfileService.getAvailableAgentsForTeam(teamId)).willReturn(availableAgents);

        // when
        orchestrator.onTicketCreated(event);

        // then
        then(agentProfileService).should().getAvailableAgentsForTeam(teamId);
        then(ticketService).should().evaluateAndFillAgentCapacity(firstAgentId, teamId);
        then(ticketService).should().evaluateAndFillAgentCapacity(secondAgentId, teamId);
        then(ticketService).should(times(2)).evaluateAndFillAgentCapacity(any(), eq(teamId));
    }
}
