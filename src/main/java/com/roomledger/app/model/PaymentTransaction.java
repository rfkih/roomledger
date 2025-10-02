package com.roomledger.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "payment_transactions",
//        uniqueConstraints = @UniqueConstraint(name = "uk_ptrx_provider_payment_id", columnNames = "provider_payment_id"),
        indexes = {
                @Index(name = "ix_ptrx_payment", columnList = "payment_id"),
                @Index(name = "ix_ptrx_code", columnList = "customer_payment_code_id"),
                @Index(name = "ix_ptrx_status", columnList = "status")
        }
)
@Getter @Setter
@EntityListeners(AuditingEntityListener.class)
public class PaymentTransaction extends Audit {


    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_payment_code_id")
    private CustomerPaymentCode customerPaymentCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(nullable = false, length = 16)
    private String provider = "XENDIT";

    @Column(name = "provider_payment_id", length = 64)
    private String providerPaymentId; // unique per capture

    @Column(name = "payment_request_id", length = 64)
    private String paymentRequestId;

    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(name = "channel_code", length = 64)
    private String channelCode;


    @Column(name = "type", length = 64)
    private String type;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 3, nullable = false)
    private String currency = "IDR";

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "fee_amount")
    private Long feeAmount;

    @Column(name = "net_amount")
    private Long netAmount;

    @Column(name = "total_amount")
    private Long totalAmount;

//    @Type(JsonType.class)
//    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
//    private JsonNode payload; // raw webhook for audit

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

}
