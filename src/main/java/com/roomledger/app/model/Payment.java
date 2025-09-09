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
@Table(name = "payments")
@Setter
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Payment {
    public enum Type { DEPOSIT, RENT }
    public enum Status { PENDING, PAID, VERIFIED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private String method;
    private String reference;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "period_month")
    private LocalDate periodMonth;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at") // nullable is fine
    private LocalDateTime updatedAt;
}

