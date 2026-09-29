package com.nn.ticketapp_api.shared.messaging.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMqProperties(
        @NotBlank(message = "Exchange name must not be blank")
        String exchange,
        @NotNull(message = "Queues configuration must not be null")
        @Valid
        Queues queues,
        @NotNull(message = "Routing keys configuration must not be null")
        @Valid
        RoutingKeys routingKeys
) {
    public record Queues(
            @NotBlank(message = "Ticket created assignment queue name must not be blank")
            String ticketCreatedAssignment,
            @NotBlank(message = "Ticket resolved assignment queue name must not be blank")
            String ticketResolvedAssignment,
            @NotBlank(message = "Agent available assignment queue name must not be blank")
            String agentAvailableAssignment,
            @NotBlank(message = "Ticket created notification queue name must not be blank")
            String ticketCreatedNotification,
            @NotBlank(message = "Ticket resolved notification queue name must not be blank")
            String ticketResolvedNotification
    ) {
    }

    public record RoutingKeys(
            @NotBlank(message = "Ticket created routing key must not be blank")
            String ticketCreated,
            @NotBlank(message = "Ticket resolved routing key must not be blank")
            String ticketResolved,
            @NotBlank(message = "Agent available routing key must not be blank")
            String agentAvailable
    ) {
    }
}
