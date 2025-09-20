package com.roomledger.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "owner_whatsapp_numbers",
        indexes = {
                @Index(name = "ix_own_wa_owner",   columnList = "owner_id"),
                @Index(name = "ix_own_wa_building",columnList = "building_id"),
                @Index(name = "ix_own_wa_phone_id",columnList = "phone_number_id")
        })
@Getter @Setter
public class OwnerWhatsappNumber extends Audit {

    public enum Status { ACTIVE, INACTIVE }

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    // ✅ Join only by building_id (no owner_id here)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "phone_number_id", nullable = false, length = 64)
    private String phoneNumberId;

    @Column(name = "access_token_enc")
    private String accessTokenEnc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;
}


