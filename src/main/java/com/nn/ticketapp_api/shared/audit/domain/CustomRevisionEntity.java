package com.nn.ticketapp_api.shared.audit.domain;

import com.nn.ticketapp_api.shared.audit.listener.SecurityContextRevisionListener;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.util.UUID;

@Entity
@Table(name = "revinfo")
@RevisionEntity(SecurityContextRevisionListener.class)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CustomRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revinfo_seq")
    @SequenceGenerator(name = "revinfo_seq", sequenceName = "revinfo_seq", allocationSize = 1)
    @RevisionNumber
    @EqualsAndHashCode.Include
    @Column(name = "rev")
    private Integer id;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private Long timestamp;

    @Column(name = "author_id")
    private UUID authorId;
}
