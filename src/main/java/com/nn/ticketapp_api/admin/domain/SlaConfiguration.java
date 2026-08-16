package com.nn.ticketapp_api.admin.domain;

import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sla_configurations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SlaConfiguration {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private TicketPriority priority;

    @Column(name = "resolution_hours", nullable = false)
    private Integer resolutionHours;

    public static SlaConfiguration create(TicketPriority priority, Integer resolutionHours) {
        return SlaConfiguration.builder()
                .priority(priority)
                .resolutionHours(resolutionHours)
                .build();
    }

    public void updateResolutionHours(Integer newResolutionHours) {
        this.resolutionHours = newResolutionHours;
    }
}
