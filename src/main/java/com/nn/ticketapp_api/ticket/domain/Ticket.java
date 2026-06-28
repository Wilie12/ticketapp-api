package com.nn.ticketapp_api.ticket.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(name = "ticket_number", nullable = false, unique = true, updatable = false)
    @Setter(AccessLevel.NONE)
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
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

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
}
