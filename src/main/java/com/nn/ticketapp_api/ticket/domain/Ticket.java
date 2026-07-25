package com.nn.ticketapp_api.ticket.domain;

import com.nn.ticketapp_api.ticket.exception.InvalidStatusTransitionException;
import com.nn.ticketapp_api.ticket.exception.TicketClosedException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "ticket_number", nullable = false, unique = true, updatable = false)
    private String ticketNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "creator_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UUID creatorId;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Column(name = "assigned_team_id")
    private UUID assignedTeamId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public boolean isOwnedBy(UUID userId) {
        return this.creatorId.equals(userId);
    }

    public static Ticket createNew(String ticketNumber, String title, String description,
                                   TicketPriority priority, UUID creatorId, UUID assignedTeamId) {
        return Ticket.builder()
                .ticketNumber(ticketNumber)
                .title(title)
                .description(description)
                .priority(priority)
                .status(TicketStatus.NEW)
                .creatorId(creatorId)
                .assignedTeamId(assignedTeamId)
                .build();
    }

    public void assignToAgent(UUID agentId) {
        if (this.status != TicketStatus.NEW) {
            throw new InvalidStatusTransitionException(
                    String.format("Cannot assign ticket in status %s. Only NEW tickets can be assigned.", this.status)
            );
        }

        ensureStatusTransitionTo(TicketStatus.IN_PROGRESS);
        this.assignedAgentId = agentId;
        this.status = TicketStatus.IN_PROGRESS;
    }

    public void resolve() {
        ensureStatusTransitionTo(TicketStatus.RESOLVED);
        this.status = TicketStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public void close() {
        ensureStatusTransitionTo(TicketStatus.CLOSED);
        this.status = TicketStatus.CLOSED;
    }

    public void reopen() {
        if (this.status != TicketStatus.RESOLVED) {
            throw new InvalidStatusTransitionException(
                    String.format(
                            "Cannot reopen ticket in status %s. Only RESOLVED tickets can be reopened.",
                            this.status
                    )
            );
        }

        ensureStatusTransitionTo(TicketStatus.IN_PROGRESS);
        this.status = TicketStatus.IN_PROGRESS;
        this.resolvedAt = null;
    }

    public void updateDetails(String title, TicketPriority priority, UUID targetTeamId) {
        if (this.status == TicketStatus.CLOSED) {
            throw new TicketClosedException(
                    String.format("Ticket %s is CLOSED and cannot be modified.", this.ticketNumber)
            );
        }

        if (title != null && !title.isBlank()) {
            this.title = title;
        }

        if (priority != null) {
            this.priority = priority;
        }

        if (targetTeamId == null || targetTeamId.equals(this.assignedTeamId)) {
            return;
        }

        this.assignedTeamId = targetTeamId;
        this.assignedAgentId = null;
    }

    private void ensureStatusTransitionTo(TicketStatus targetStatus) {
        if (!this.status.canTransitionTo(targetStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format("Cannot transition ticket from %s to %s.", this.status, targetStatus)
            );
        }
    }
}
