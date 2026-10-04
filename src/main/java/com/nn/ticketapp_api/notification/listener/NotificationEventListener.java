package com.nn.ticketapp_api.notification.listener;

import com.nn.ticketapp_api.notification.service.EmailSender;
import com.nn.ticketapp_api.notification.template.EmailTemplateProcessor;
import com.nn.ticketapp_api.shared.identity.service.IdentityGateway;
import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import com.nn.ticketapp_api.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final TicketRepository ticketRepository;
    private final EmailTemplateProcessor emailTemplateProcessor;
    private final EmailSender emailSender;
    private final IdentityGateway identityGateway;

    @RabbitListener(queues = "${app.rabbitmq.queues.ticket-created-notification}")
    public void handleTicketCreated(TicketCreatedEvent event) {
        log.debug("Processing AMQP message for created ticket: {}", event.ticketId());

        ticketRepository.findById(event.ticketId()).ifPresent(ticket -> {
            identityGateway.getEmailById(ticket.getCreatorId()).ifPresent( email -> {
                String subject = String.format("Ticket Created: %s", ticket.getTicketNumber());

                Map<String, Object> variables = Map.of(
                        "ticketNumber", ticket.getTicketNumber(),
                        "title", ticket.getTitle(),
                        "priority", ticket.getPriority().name()
                );

                String htmlBody = emailTemplateProcessor.processTemplate("email/ticket-created", variables);
                emailSender.sendHtmlEmail(email, subject, htmlBody);
            });
        });
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.ticket-resolved-notification}")
    public void handleTicketResolved(TicketResolvedEvent event) {
        log.debug("Processing AMQP message for resolved ticket: {}", event.ticketId());

        ticketRepository.findById(event.ticketId()).ifPresent(ticket -> {
            identityGateway.getEmailById(ticket.getCreatorId()).ifPresent( email -> {
                String subject = String.format("Ticket Resolved: %s", ticket.getTicketNumber());

                Map<String, Object> variables = Map.of(
                        "ticketNumber", ticket.getTicketNumber(),
                        "resolutionNote", event.resolutionNote()
                );

                String htmlBody = emailTemplateProcessor.processTemplate("email/ticket-resolved", variables);
                emailSender.sendHtmlEmail(email, subject, htmlBody);
            });
        });
    }
}
