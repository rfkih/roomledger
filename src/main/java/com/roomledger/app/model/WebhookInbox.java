package com.roomledger.app.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "webhook_inbox",
        uniqueConstraints = @UniqueConstraint(name = "uk_webhook_provider_event", columnNames = {"provider","event_id"})
)
@Getter @Setter
@EntityListeners(AuditingEntityListener.class)
public class WebhookInbox  extends Audit {

    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 16)
    private String provider; // "XENDIT"

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode payload;

    @Column(nullable = false)
    private boolean processed = false;

    @CreatedDate
    @Column(name="received_at", updatable=false, nullable=false)
    private LocalDateTime receivedAt;


}
