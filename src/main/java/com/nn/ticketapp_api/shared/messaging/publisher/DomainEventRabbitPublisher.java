package com.nn.ticketapp_api.shared.messaging.publisher;

import com.nn.ticketapp_api.agent.domain.event.AgentAvailableEvent;
import com.nn.ticketapp_api.shared.messaging.config.RabbitMqProperties;
import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventRabbitPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(TicketCreatedEvent event) {
        log.debug("Bridging TicketCreatedEvent to RabbitMQ for ticket: {}", event.ticketId());

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routingKeys().ticketCreated(),
                event
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(TicketResolvedEvent event) {
        log.debug("Bridging TicketResolvedEvent to RabbitMQ for ticket: {}", event.ticketId());

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routingKeys().ticketResolved(),
                event
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(AgentAvailableEvent event) {
        log.debug("Bridging AgentAvailableEvent to RabbitMQ for ticket: {}", event.agentId());

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routingKeys().agentAvailable(),
                event
        );
    }
}
