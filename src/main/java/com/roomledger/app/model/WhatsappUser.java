package com.roomledger.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_user")
@Setter
@Getter
public class WhatsappUser {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "wa_id", length = 20, nullable = false, unique = true)
    private String waId;

    @Column(length = 100)
    private String name;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt = LocalDateTime.now();

}

