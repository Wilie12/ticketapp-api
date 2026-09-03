package com.nn.ticketapp_api.notification.listener;

import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.agent.domain.AgentProfile;
import com.nn.ticketapp_api.agent.repository.AgentProfileRepository;
import com.nn.ticketapp_api.notification.service.EmailSender;
import com.nn.ticketapp_api.team.domain.Team;
import com.nn.ticketapp_api.team.repository.TeamRepository;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import com.nn.ticketapp_api.ticket.domain.TicketStatus;
import com.nn.ticketapp_api.ticket.domain.event.TicketCreatedEvent;
import com.nn.ticketapp_api.ticket.domain.event.TicketResolvedEvent;
import com.nn.ticketapp_api.ticket.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.timeout;

public class NotificationEventListenerTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private AgentProfileRepository agentProfileRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private EmailSender emailSender;

    @AfterEach
    void tearDown() {
        ticketRepository.deleteAllInBatch();
        agentProfileRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should process TicketCreatedEvent asynchronously after transaction commit and send email")
    void shouldProcessTicketCreatedEventAndSendEmail() {
        // given
        Team team = Team.create("Infrastructure", "Handles backend issues");
        teamRepository.saveAndFlush(team);

        UUID creatorId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .ticketNumber("INC0000123")
                .title("VPN Connection Issue")
                .description("Cannot connect to VPN")
                .priority(TicketPriority.HIGH)
                .status(TicketStatus.NEW)
                .creatorId(creatorId)
                .assignedTeamId(team.getId())
                .build();
        ticketRepository.saveAndFlush(ticket);

        TicketCreatedEvent event = new TicketCreatedEvent(ticket.getId(), ticket.getAssignedTeamId());

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        // then
        String expectedRecipient = String.format("user-%s@ticketapp.local", creatorId);
        String expectedSubject = "Ticket Created: INC0000123";

        then(emailSender).should(timeout(2000)).sendHtmlEmail(
                eq(expectedRecipient),
                eq(expectedSubject),
                contains("VPN Connection Issue")
        );
    }

    @Test
    @DisplayName("Should process TicketResolvedEvent asynchronously after transaction commit and send email")
    void shouldProcessTicketResolvedEventAndSendEmail() {
        // given
        Team team = Team.create("Network", "Handles connectivity issues");
        teamRepository.saveAndFlush(team);

        UUID agentId = UUID.randomUUID();
        AgentProfile agent = AgentProfile.create(agentId, team.getId());
        agentProfileRepository.saveAndFlush(agent);

        UUID creatorId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .ticketNumber("INC0000124")
                .title("Network Issue")
                .description("Network is down")
                .priority(TicketPriority.CRITICAL)
                .status(TicketStatus.RESOLVED)
                .creatorId(creatorId)
                .assignedTeamId(team.getId())
                .assignedAgentId(agent.getId())
                .build();
        ticketRepository.saveAndFlush(ticket);

        TicketResolvedEvent event = new TicketResolvedEvent(
                ticket.getId(),
                ticket.getAssignedAgentId(),
                ticket.getAssignedTeamId(),
                "Restarted the core router."
        );

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        // then
        String expectedRecipient = String.format("user-%s@ticketapp.local", creatorId);
        String expectedSubject = "Ticket Resolved: INC0000124";

        then(emailSender).should(timeout(2000)).sendHtmlEmail(
                eq(expectedRecipient),
                eq(expectedSubject),
                contains("Restarted the core router.")
        );
    }
}
