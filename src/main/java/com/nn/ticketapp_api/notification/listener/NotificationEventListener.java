package com.nn.ticketapp_api.notification.listener;

import com.nn.ticketapp_api.notification.service.EmailSender;
import com.nn.ticketapp_api.notification.template.EmailTemplateProcessor;
import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import com.nn.ticketapp_api.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final TicketRepository ticketRepository;
    private final EmailTemplateProcessor emailTemplateProcessor;
    private final EmailSender emailSender;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketCreated(TicketCreatedEvent event) {
        log.debug("Processing asynchronous email notification for created ticket: {}", event.ticketId());

        ticketRepository.findById(event.ticketId()).ifPresent(ticket -> {
            String recipient = String.format("user-%s@ticketapp.local", ticket.getCreatorId());
            String subject = String.format("Ticket Created: %s", ticket.getTicketNumber());

            Map<String, Object> variables = Map.of(
                    "ticketNumber", ticket.getTicketNumber(),
                    "title", ticket.getTitle(),
                    "priority", ticket.getPriority().name()
            );

            String htmlBody = emailTemplateProcessor.processTemplate("email/ticket-created", variables);
            emailSender.sendHtmlEmail(recipient, subject, htmlBody);
        });
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketResolved(TicketResolvedEvent event) {
        log.debug("Processing asynchronous email notification for resolved ticket: {}", event.ticketId());

        ticketRepository.findById(event.ticketId()).ifPresent(ticket -> {
            String recipient = String.format("user-%s@ticketapp.local", ticket.getCreatorId());
            String subject = String.format("Ticket Resolved: %s", ticket.getTicketNumber());

            Map<String, Object> variables = Map.of(
                    "ticketNumber", ticket.getTicketNumber(),
                    "resolutionNote", event.resolutionNote()
            );

            String htmlBody = emailTemplateProcessor.processTemplate("email/ticket-resolved", variables);
            emailSender.sendHtmlEmail(recipient, subject, htmlBody);
        });
    }
}
