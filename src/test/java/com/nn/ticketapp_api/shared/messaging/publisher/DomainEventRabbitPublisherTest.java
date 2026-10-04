package com.nn.ticketapp_api.shared.messaging.publisher;

import com.nn.ticketapp_api.agent.domain.event.AgentAvailableEvent;
import com.nn.ticketapp_api.shared.messaging.config.RabbitMqProperties;
import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class DomainEventRabbitPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    private DomainEventRabbitPublisher publisher;

    @BeforeEach
    void setUp() {
        RabbitMqProperties.Queues queues = new RabbitMqProperties.Queues(
                "q-tc-assign",
                "q-tr-assign",
                "q-aa-assign",
                "q-tc-notif",
                "q-tr-notif"
        );
        RabbitMqProperties.RoutingKeys routingKeys = new RabbitMqProperties.RoutingKeys(
                "ticket.created",
                "ticket.resolved",
                "agent.available"
        );
        RabbitMqProperties properties = new RabbitMqProperties("test.exchange", queues, routingKeys);

        publisher = new DomainEventRabbitPublisher(rabbitTemplate, properties);
    }

    @Test
    @DisplayName("Should route TicketCreatedEvent to correct exchange and routing key")
    void shouldRouteTicketCreatedEvent() {
        // given
        TicketCreatedEvent event = new TicketCreatedEvent(UUID.randomUUID(), UUID.randomUUID());

        // when
        publisher.publish(event);

        // then
        then(rabbitTemplate).should().convertAndSend("test.exchange", "ticket.created", event);
    }

    @Test
    @DisplayName("Should route TicketResolvedEvent to correct exchange and routing key")
    void shouldRouteTicketResolvedEvent() {
        // given
        TicketResolvedEvent event = new TicketResolvedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "System restarted."
        );

        // when
        publisher.publish(event);

        // then
        then(rabbitTemplate).should().convertAndSend("test.exchange", "ticket.resolved", event);
    }

    @Test
    @DisplayName("Should route AgentAvailableEvent to correct exchange and routing key")
    void shouldRouteAgentAvailableEvent() {
        // given
        AgentAvailableEvent event = new AgentAvailableEvent(UUID.randomUUID(), UUID.randomUUID());

        // when
        publisher.publish(event);

        // then
        then(rabbitTemplate).should().convertAndSend("test.exchange", "agent.available", event);
    }
}
