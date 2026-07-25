package com.nn.ticketapp_api.communication.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private UUID ticketId;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(name = "original_filename", nullable = false, updatable = false)
    private String originalFilename;

    @Column(name = "object_key", nullable = false, updatable = false, unique = true)
    private String objectKey;

    @Column(name = "content_type", nullable = false, updatable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false, updatable = false)
    private Long fileSize;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    public static Attachment create(
            UUID ticketId,
            UUID authorId,
            String originalFilename,
            String objectKey,
            String contentType,
            Long fileSize
    ) {
        return Attachment.builder()
                .ticketId(ticketId)
                .authorId(authorId)
                .originalFilename(originalFilename)
                .objectKey(objectKey)
                .contentType(contentType)
                .fileSize(fileSize)
                .build();
    }
}
