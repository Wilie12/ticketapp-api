package com.nn.ticketapp_api.agent.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AgentProfile {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentStatus status;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AgentProfile create(UUID id, UUID teamId) {
        return AgentProfile.builder()
                .id(id)
                .teamId(teamId)
                .status(AgentStatus.OFFLINE)
                .build();
    }

    public void updateStatus(AgentStatus newStatus) {
        if (this.status != newStatus) {
            this.status = newStatus;
        }
    }

    public void changeTeamId(UUID newTeamId) {
        this.teamId = newTeamId;
    }
}
