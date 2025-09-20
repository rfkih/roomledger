package com.roomledger.app.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_ptrx_provider_payment_id", columnNames = "provider_payment_id"),
        indexes = {
                @Index(name = "ix_ptrx_payment", columnList = "payment_id"),
                @Index(name = "ix_ptrx_code", columnList = "customer_payment_code_id"),
                @Index(name = "ix_ptrx_status", columnList = "status")
        }
)
@Getter @Setter
@EntityListeners(AuditingEntityListener.class)
public class PaymentTransaction {

    public enum Status { PAID, FAILED, REFUNDED }

    @Id @GeneratedValue
    private UUID id;

    // Optional link to your Payments table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    // Or link to the reusable code (when you’re using VA/QR per customer)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_payment_code_id")
    private CustomerPaymentCode customerPaymentCode;

    @Column(nullable = false, length = 16)
    private String provider = "XENDIT";

    @Column(name = "provider_payment_id", nullable = false, length = 64)
    private String providerPaymentId; // unique per capture

    @Column(name = "payment_request_id", length = 64)
    private String paymentRequestId;

    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(name = "channel_code", nullable = false, length = 64)
    private String channelCode;

    @Column(nullable = false)
    private Long amount; // BIGINT (minor units)

    @Column(length = 3, nullable = false)
    private String currency = "IDR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "fee_amount")
    private Long feeAmount;

    @Column(name = "net_amount")
    private Long netAmount;

    @Type(JsonType.class)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private JsonNode payload; // raw webhook for audit

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
