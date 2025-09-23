package com.roomledger.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProvisionReusableQrisRequest(
        @NotNull UUID ownerId,
        UUID buildingId,
        @NotBlank String customerId
) {}