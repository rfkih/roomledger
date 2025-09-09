package com.roomledger.app.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity @Table(name = "app_params")
@Setter
@Getter
public class AppParam {
    @Id
    private String key;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String value;

    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
