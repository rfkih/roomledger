package com.roomledger.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(
        name = "payment_attempts",
        indexes = {
                @Index(name = "ix_attempts_booking", columnList = "booking_id")
        }
)
@Getter @Setter
@EntityListeners(AuditingEntityListener.class)
public class PaymentAttempt extends Audit {

    public enum Type { PAY, REUSABLE_PAYMENT_CODE }
    public enum Status { PENDING, PAID, FAILED, EXPIRED }

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "booking_id", nullable = false, length = 64)
    private String bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(name = "qris_qr_string", length = 60)
    private String qrisQrString;

    @Column(name = "va_number", length = 32)
    private String vaNumber;


    @Column(name = "channel_code", nullable = false, length = 64)
    private String channelCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "request_amount")
    private Long requestAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "pr_id", length = 64)
    private String prId; // payment_request_id

    @Column(name = "payment_id", length = 64)
    private String paymentId; // capture/payment id (when available)

    @Column(name = "idem_key")
    private UUID idemKey; // Idempotency-Key

//    @Type(JsonType.class)
//    @Column(columnDefinition = "actions", nullable = false)
//    private JsonNode actions;

}
