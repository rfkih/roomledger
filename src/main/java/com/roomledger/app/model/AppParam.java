package com.roomledger.app.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity @Table(name = "app_params")
@Setter
@Getter
public class AppParam extends Audit {
    @Id
    private String key;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String value;
}
