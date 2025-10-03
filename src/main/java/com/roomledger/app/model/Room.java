package com.roomledger.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roomledger.app.model.Commons.Enum.RoomStatus;
import com.roomledger.app.model.Commons.Enum.RoomType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Setter
@Getter
@Table(name = "rooms")
public class Room extends Audit {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "building_id")
    @JsonIgnore
    private Building building;

    @Column(name = "room_no", nullable = false, unique = true)
    private String roomNo;

    @Column(name = "monthly_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.AVAILABLE;

    // ---------- NEW FIELDS ----------
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 32)
    private RoomType roomType = RoomType.STUDIO;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 1;                   // number of occupants

    @Column(name = "size_m2", precision = 8, scale = 2)
    private BigDecimal sizeM2;
}

