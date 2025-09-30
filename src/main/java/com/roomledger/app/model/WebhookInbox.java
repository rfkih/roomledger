package com.roomledger.app.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_inbox",
        uniqueConstraints = @UniqueConstraint(name = "uk_inbox_provider_event", columnNames = {"provider","event_id"}))
@Getter @Setter
public class WebhookInbox extends Audit {

    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 16)
    private String provider;                  // "XENDIT"

    @Column(name = "payment_id", nullable = false, length = 128)
    private String paymentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private JsonNode payload;                 // <-- MUST NOT BE NULL

    @Column(nullable = false)
    private boolean processed = false;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();
}
