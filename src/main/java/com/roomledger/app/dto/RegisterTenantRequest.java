package com.roomledger.app.dto;


import jakarta.validation.constraints.NotBlank;

public record RegisterTenantRequest(
        @NotBlank String name,
        @NotBlank String no_id,
        @NotBlank String gender,
        @NotBlank String phone
) {}

