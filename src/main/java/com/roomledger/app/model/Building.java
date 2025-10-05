package com.roomledger.app.model;

import com.roomledger.app.model.Commons.Enum.BuildingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "buildings",
        uniqueConstraints = @UniqueConstraint(name = "uk_buildings_owner_code", columnNames = {"owner_id","code"}),
        indexes = { @Index(name = "ix_building_owner", columnList = "owner_id"),
                @Index(name = "ix_building_status", columnList = "status") })
@Getter @Setter
@EntityListeners(AuditingEntityListener.class)
public class Building extends Audit {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String address;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private BuildingStatus status = BuildingStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_whatsapp_id")
    private OwnerWhatsappNumber defaultWhatsapp;
}
