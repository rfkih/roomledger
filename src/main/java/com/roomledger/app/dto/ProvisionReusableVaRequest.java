package com.roomledger.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProvisionReusableVaRequest(
        @NotNull UUID ownerId,
        UUID buildingId,
        @NotBlank String customerId,
        @NotBlank String displayName,
        @NotBlank String channelCode
) {}
