package com.roomledger.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity @Table(name = "tenants")
@Setter
@Getter
public class Tenant extends Audit {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String no_id;

    @Column(nullable = false)
    private String gender;

    @Column(unique = true)
    private String phone;

}

