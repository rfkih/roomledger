package com.roomledger.app.model;

import com.roomledger.app.model.Commons.Enum.OwnerStatus;
import com.roomledger.app.model.Commons.Enum.OwnerType;
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
@Table(name = "owners", indexes = @Index(name = "ix_owner_slug", columnList = "slug", unique = true))
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Owner extends Audit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 50, unique = true)
    private String slug;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private OwnerType type = OwnerType.PERSON;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private OwnerStatus status = OwnerStatus.ACTIVE;

    @Column(nullable = false, length = 64)
    private String timezone = "Asia/Jakarta";

    @Column(name = "xendit_sub_id", length = 64)
    private String xenditSubId;
}

