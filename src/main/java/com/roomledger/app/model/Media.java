package com.roomledger.app.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "media",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_media_storage_key", columnNames = "storage_key")
        }
)
@Setter
@Getter
public class Media {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "bucket", nullable = false)
    private String bucket;                // rl-public / rl-private

    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;            // full object key

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "visibility", nullable = false)
    private String visibility;            // 'PUBLIC' | 'SENSITIVE' (DB check enforces)

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}

