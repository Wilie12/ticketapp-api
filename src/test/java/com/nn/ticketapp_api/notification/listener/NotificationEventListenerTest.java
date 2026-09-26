package com.nn.ticketapp_api.notification.listener;

import com.nn.ticketapp_api.notification.service.EmailSender;
import com.nn.ticketapp_api.notification.template.EmailTemplateProcessor;
import com.nn.ticketapp_api.shared.identity.service.IdentityGateway;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import com.nn.ticketapp_api.ticket.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class NotificationEventListenerTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private EmailTemplateProcessor emailTemplateProcessor;
    @Mock
    private EmailSender emailSender;
    @Mock
    private IdentityGateway identityGateway;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    @Test
    @DisplayName("Should process TicketCreatedEvent asynchronously after transaction commit and send email")
    void shouldProcessTicketCreatedEventAndSendEmail() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ticketNumber("INC0000123")
                .title("VPN Connection Issue")
                .description("Cannot connect to VPN")
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.NEW)
                .creatorId(creatorId)
                .assignedTeamId(teamId)
                .build();

        TicketCreatedEvent event = new TicketCreatedEvent(ticketId, teamId);

        String expectedRecipient = String.format("user-%s@ticketapp.local", creatorId);
        String expectedHtmlBody = "<html>Ticket INC0000123 Created</html>";

        Map<String, Object> expectedVariables = Map.of(
                "ticketNumber", "INC0000123",
                "title", "VPN Connection Issue",
                "priority", "HIGH"
        );

        given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticket));
        given(identityGateway.getEmailById(creatorId)).willReturn(Optional.of(expectedRecipient));
        given(emailTemplateProcessor.processTemplate(eq("email/ticket-created"), eq(expectedVariables)))
                .willReturn(expectedHtmlBody);

        // when
        notificationEventListener.handleTicketCreated(event);

        // then
        then(ticketRepository).should().findById(ticketId);
        then(identityGateway).should().getEmailById(creatorId);
        then(emailTemplateProcessor).should().processTemplate("email/ticket-created", expectedVariables);
        then(emailSender).should().sendHtmlEmail(
                expectedRecipient,
                "Ticket Created: INC0000123",
                expectedHtmlBody
        );
    }

    @Test
    @DisplayName("Should process TicketResolvedEvent asynchronously after transaction commit and send email")
    void shouldProcessTicketResolvedEventAndSendEmail() {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        String resolutionNote = "Restarted the core router.";

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ticketNumber("INC0000124")
                .title("Network Issue")
                .description("Network is down")
                .priority(TicketPriority.CRITICAL)
                .status(TicketStatus.RESOLVED)
                .creatorId(creatorId)
                .assignedTeamId(teamId)
                .assignedAgentId(agentId)
                .build();

        TicketResolvedEvent event = new TicketResolvedEvent(ticketId, agentId, teamId, resolutionNote);
        String expectedRecipient = String.format("user-%s@ticketapp.local", creatorId);
        String expectedHtmlBody = "<html>Ticket INC0000124 Resolved</html>";

        Map<String, Object> expectedVariables = Map.of(
                "ticketNumber", "INC0000124",
                "resolutionNote", resolutionNote
        );

        given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticket));
        given(identityGateway.getEmailById(creatorId)).willReturn(Optional.of(expectedRecipient));
        given(emailTemplateProcessor.processTemplate(eq("email/ticket-resolved"), eq(expectedVariables)))
                .willReturn(expectedHtmlBody);

        // when
        notificationEventListener.handleTicketResolved(event);

        // then
        then(ticketRepository).should().findById(ticketId);
        then(identityGateway).should().getEmailById(creatorId);
        then(emailTemplateProcessor).should().processTemplate("email/ticket-resolved", expectedVariables);
        then(emailSender).should().sendHtmlEmail(
                expectedRecipient,
                "Ticket Resolved: INC0000124",
                expectedHtmlBody
        );
    }
}
