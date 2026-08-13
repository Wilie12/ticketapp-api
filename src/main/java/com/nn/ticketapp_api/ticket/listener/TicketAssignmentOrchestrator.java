package com.nn.ticketapp_api.ticket.listener;

import com.nn.ticketapp_api.agent.domain.event.AgentAvailableEvent;
import com.nn.ticketapp_api.agent.service.AgentProfileService;
import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import com.nn.ticketapp_api.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketAssignmentOrchestrator {

    private final TicketService ticketService;
    private final AgentProfileService agentProfileService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgentAvailable(AgentAvailableEvent event) {
        log.debug("Agent {} became AVAILABLE: Evaluating queue for team {}", event.agentId(), event.teamId());

        ticketService.evaluateAndFillAgentCapacity(event.agentId(), event.teamId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketResolved(TicketResolvedEvent event) {
        log.debug(
                "Agent {} resolved ticket {}. Evaluating queue for team {}",
                event.agentId(),
                event.ticketId(),
                event.teamId()
        );

        ticketService.evaluateAndFillAgentCapacity(event.agentId(), event.teamId());
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(TicketCreatedEvent event) {
        log.debug("New ticket {} created in team {}. Searching for available agents.", event.teamId(), event.teamId());

        List<UUID> availableAgents = agentProfileService.getAvailableAgentsForTeam(event.teamId());

        for (UUID agentId : availableAgents) {
            ticketService.evaluateAndFillAgentCapacity(agentId, event.teamId());
        }
    }
}
