package com.roomledger.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "media_link",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_media_link_order", columnNames = {"owner_type","owner_id","sort_order"}),
                @UniqueConstraint(name = "uq_media_link_media", columnNames = {"owner_type","owner_id","media_id"})
        }
)
@Setter
@Getter
public class MediaLink {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "owner_type", nullable = false)
    private String ownerType;        // e.g., ROOM | BUILDING | CUSTOMER

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(name = "purpose", nullable = false)
    private String purpose;          // e.g., ROOM_PHOTO, CUSTOMER_ID_FRONT

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_main", nullable = false)
    private boolean isMain = false;

    @Column(name = "caption")
    private String caption;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

}

