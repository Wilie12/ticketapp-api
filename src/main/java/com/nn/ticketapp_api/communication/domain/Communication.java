package com.nn.ticketapp_api.communication.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "communications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Communication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private UUID ticketId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CommunicationType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Communication createComment(UUID ticketId, UUID authorId, String content) {
        return Communication.builder()
                .ticketId(ticketId)
                .authorId(authorId)
                .content(content)
                .type(CommunicationType.PUBLIC_COMMENT)
                .build();
    }

    public static Communication createWorkNote(UUID ticketId, UUID authorId, String content) {
        return Communication.builder()
                .ticketId(ticketId)
                .authorId(authorId)
                .content(content)
                .type(CommunicationType.WORK_NOTE)
                .build();
    }

    public static Communication createSystemEvent(UUID ticketId, UUID authorId, String content) {
        return Communication.builder()
                .ticketId(ticketId)
                .authorId(authorId)
                .content(content)
                .type(CommunicationType.SYSTEM_EVENT)
                .build();
    }
}
