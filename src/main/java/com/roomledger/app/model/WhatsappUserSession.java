package com.roomledger.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(
        name = "whatsapp_user_session",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "wa_phone_id"})
        }
)
public class WhatsappUserSession {

    @Id
    @GeneratedValue
    private UUID id;

    // --- Relations ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private WhatsappUser user;

    @Column(name = "wa_phone_id", nullable = false)
    private UUID waPhoneId; // references owner_whatsapp_numbers.id

    @Column(name = "building_id")
    private UUID buildingId; // references buildings.id

    // --- Attributes ---
    @Column(length = 50)
    private String intent;

    @Column(length = 50)
    private String state = "START";

    @Column(columnDefinition = "jsonb")
    private String contextData = "{}";

    @Column(name = "is_human_handover")
    private boolean isHumanHandover = false;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "last_interaction_at")
    private LocalDateTime lastInteractionAt = LocalDateTime.now();

    @Column(name = "created_at")
    private LocalDateTime createdAt;


}

