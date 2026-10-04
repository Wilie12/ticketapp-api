package com.nn.ticketapp_api.shared.messaging.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final RabbitMqProperties properties;

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public TopicExchange domainEventsExchange() {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    public Queue ticketCreatedAssignmentQueue() {
        return QueueBuilder.durable(properties.queues().ticketCreatedAssignment()).build();
    }

    @Bean
    public Queue ticketResolvedAssignmentQueue() {
        return QueueBuilder.durable(properties.queues().ticketResolvedAssignment()).build();
    }

    @Bean
    public Queue agentAvailableAssignmentQueue() {
        return QueueBuilder.durable(properties.queues().agentAvailableAssignment()).build();
    }

    @Bean
    public Queue ticketCreatedNotificationQueue() {
        return QueueBuilder.durable(properties.queues().ticketCreatedNotification()).build();
    }

    @Bean
    public Queue ticketResolvedNotificationQueue() {
        return QueueBuilder.durable(properties.queues().ticketResolvedNotification()).build();
    }

    @Bean
    public Binding ticketCreatedAssignmentBinding(
            Queue ticketCreatedAssignmentQueue,
            TopicExchange domainEventsExchange
    ) {
        return BindingBuilder.bind(ticketCreatedAssignmentQueue)
                .to(domainEventsExchange)
                .with(properties.routingKeys().ticketCreated());
    }

    @Bean
    public Binding ticketResolvedAssignmentBinding(
            Queue ticketResolvedAssignmentQueue,
            TopicExchange domainEventsExchange
    ) {
        return BindingBuilder.bind(ticketResolvedAssignmentQueue)
                .to(domainEventsExchange)
                .with(properties.routingKeys().ticketResolved());
    }

    @Bean
    public Binding agentAvailableAssignmentBinding(
            Queue agentAvailableAssignmentQueue,
            TopicExchange domainEventsExchange
    ) {
        return BindingBuilder.bind(agentAvailableAssignmentQueue)
                .to(domainEventsExchange)
                .with(properties.routingKeys().agentAvailable());
    }

    @Bean
    public Binding ticketCreatedNotificationBinding(
            Queue ticketCreatedNotificationQueue,
            TopicExchange domainEventsExchange
    ) {
        return BindingBuilder.bind(ticketCreatedNotificationQueue)
                .to(domainEventsExchange)
                .with(properties.routingKeys().ticketCreated());
    }

    @Bean
    public Binding ticketResolvedNotificationBinding(
            Queue ticketResolvedNotificationQueue,
            TopicExchange domainEventsExchange
    ) {
        return BindingBuilder.bind(ticketResolvedNotificationQueue)
                .to(domainEventsExchange)
                .with(properties.routingKeys().ticketResolved());
    }
}
