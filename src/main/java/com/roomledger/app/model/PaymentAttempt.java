package com.roomledger.app.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment_attempts",
        indexes = {
                @Index(name = "ix_attempts_booking", columnList = "booking_id")
        }
        // Note: partial unique indexes (WHERE pr_id IS NOT NULL) cannot be expressed in JPA annotations;
        // they already exist in your SQL migration.
)
@Getter @Setter
@EntityListeners(AuditingEntityListener.class)
public class PaymentAttempt {

    public enum Type { PAY, REUSABLE_PAYMENT_CODE }
    public enum Status { PENDING, PAID, FAILED, EXPIRED }

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "booking_id", nullable = false, length = 64)
    private String bookingId;

    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(name = "channel_code", nullable = false, length = 64)
    private String channelCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "request_amount")
    private Long requestAmount; // BIGINT in DB (minor units)

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "pr_id", length = 64)
    private String prId; // payment_request_id

    @Column(name = "payment_id", length = 64)
    private String paymentId; // capture/payment id (when available)

    @Column(name = "idem_key")
    private UUID idemKey; // Idempotency-Key you used when creating


//    @Type(JsonType.class)
//    @Column(columnDefinition = "actions", nullable = false)
//    private JsonNode actions;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
