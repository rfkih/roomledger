package com.roomledger.app.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roomledger.app.model.Commons.Enum.RoomStatus;
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

//    public enum Status { AVAILABLE, OCCUPIED, MAINTENANCE }

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
}

