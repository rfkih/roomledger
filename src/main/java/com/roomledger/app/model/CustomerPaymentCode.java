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
        name = "customer_payment_codes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cpc_customer_channel", columnNames = {"customer_id","channel_code"}),
                @UniqueConstraint(name = "uk_cpc_code_value", columnNames = {"code_value"})
        },
        indexes = {
                @Index(name = "ix_cpc_customer", columnList = "customer_id"),
                @Index(name = "ix_cpc_status", columnList = "status"),
                @Index(name = "ix_cpc_channel", columnList = "channel_code")
        }
)
@Getter @Setter
@EntityListeners(AuditingEntityListener.class)
public class CustomerPaymentCode extends Audit {

    public enum Kind { VIRTUAL_ACCOUNT, QR }
    public enum Status { ACTIVE, INACTIVE, EXPIRED }

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "channel_code", nullable = false, length = 64)
    private String channelCode; // e.g. BNI_VIRTUAL_ACCOUNT, ID_QRIS

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Kind kind;

    @Column(name = "code_value", nullable = false, columnDefinition = "text")
    private String codeValue; // VA number (digits) or QR string

    @Column(name = "payment_request_id", length = 64)
    private String paymentRequestId; // pr_id

    @Column(name = "payment_method_id", length = 64)
    private String paymentMethodId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.ACTIVE;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Type(JsonType.class)
    @Column(name = "actions", columnDefinition = "jsonb")
    private JsonNode actions; // raw actions[]

    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;

}
