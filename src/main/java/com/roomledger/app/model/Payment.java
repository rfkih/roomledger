package com.roomledger.app.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "ix_pay_booking", columnList = "booking_id"),
                @Index(name = "ux_pay_pr_id", columnList = "prId", unique = true),          // Xendit payment_request_id
                @Index(name = "ux_pay_provider_payment", columnList = "providerPaymentId", unique = true)
        }
)
@Setter @Getter
@EntityListeners(AuditingEntityListener.class)
public class Payment extends Audit {

    public enum Type { DEPOSIT, RENT, FULL }

    public enum Status {
        PENDING,                // created locally, before calling Xendit
        WAITING_FOR_PAYMENT,    // created after (Inquiry) Payment Committed, waiting customer action to pay
        PAID,                   // paid (from webhook)
        FAILED,                 // failed (from webhook)
        EXPIRED,                // expired (from webhook)
        CANCELLED,              //  cancelled
        VERIFIED                // verified
    }

    public enum GatewayFlow { PAY, REUSABLE_PAYMENT_CODE } // Xendit flow types

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // --- (legacy) optional: keep but prefer new fields below
    @Deprecated private String method;     // prefer channelCode
    @Deprecated private String reference;  // prefer referenceId

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "period_month")
    private LocalDate periodMonth;

    // ===== Gateway/Xendit fields =====
    @Column(length = 16, nullable = false)
    private String provider = "XENDIT";          // if you add more providers later

    @Enumerated(EnumType.STRING)
    @Column(name = "flow", length = 32)
    private GatewayFlow flow;                    // PAY / REUSABLE_PAYMENT_CODE

    @Column(name = "channel_code", length = 64)
    private String channelCode;                  // e.g. BCA_VIRTUAL_ACCOUNT, ID_QRIS

    @Column(name = "reference_id", length = 128)
    private String referenceId;                  // maps to Xendit reference_id (use bookingId or customerId)

    @Column(name = "prId", length = 64)
    private String prId;                         // payment_request_id from Xendit

    @Column(name = "providerPaymentId", length = 64)
    private String providerPaymentId;            // capture/payment id from webhook (if present)

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;               // the Idempotency-Key you used on create

    // What you show to users
    @Column(name = "invoice_url", length = 512)
    private String invoiceUrl;                   // if you also use Payment Links

    @Column(name = "qris_qr_string", columnDefinition = "text")
    private String qrisQrString;                 // for QRIS flow

    @Column(name = "va_number", length = 32)
    private String vaNumber;                     // for VA flows

    @Column(name = "currency", length = 3)
    private String currency = "IDR";

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "actions_json", columnDefinition = "text")
    private String actionsJson;                  // raw actions[] from Xendit response

    @Column(name = "channel_properties_json", columnDefinition = "text")
    private String channelPropertiesJson;        // what you sent/received (optional)

}


