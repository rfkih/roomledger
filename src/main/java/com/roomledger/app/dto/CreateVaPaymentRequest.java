package com.roomledger.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateVaPaymentRequest(
        @NotNull @Positive Long amount,
        @NotBlank String channelCode,        // e.g. "BNI_VIRTUAL_ACCOUNT"
        @NotBlank String displayName,        // customer display name on VA
        @NotBlank String referenceId,
        Long expectedAmount
) {}