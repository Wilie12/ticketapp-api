package com.nn.ticketapp_api.ticket.listener;

import com.nn.ticketapp_api.agent.domain.event.AgentAvailableEvent;
import com.nn.ticketapp_api.agent.service.AgentProfileService;
import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import com.nn.ticketapp_api.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketAssignmentOrchestrator {

    private final TicketService ticketService;
    private final AgentProfileService agentProfileService;

    @RabbitListener(queues = "${app.rabbitmq.queues.agent-available-assignment}")
    public void onAgentAvailable(AgentAvailableEvent event) {
        log.debug(
                "AMQP Trigger: Agent {} became AVAILABLE: Evaluating queue for team {}",
                event.agentId(),
                event.teamId()
        );

        ticketService.evaluateAndFillAgentCapacity(event.agentId(), event.teamId());
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.ticket-resolved-assignment}")
    public void onTicketResolved(TicketResolvedEvent event) {
        log.debug(
                "AMQP Trigger: Agent {} resolved ticket {}. Evaluating queue for team {}",
                event.agentId(),
                event.ticketId(),
                event.teamId()
        );

        ticketService.evaluateAndFillAgentCapacity(event.agentId(), event.teamId());
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.ticket-created-assignment}")
    public void onTicketCreated(TicketCreatedEvent event) {
        log.debug(
                "AMQP Trigger: New ticket {} created in team {}. Searching for available agents.",
                event.ticketId(),
                event.teamId()
        );

        List<UUID> availableAgents = agentProfileService.getAvailableAgentsForTeam(event.teamId());

        for (UUID agentId : availableAgents) {
            ticketService.evaluateAndFillAgentCapacity(agentId, event.teamId());
        }
    }
}
