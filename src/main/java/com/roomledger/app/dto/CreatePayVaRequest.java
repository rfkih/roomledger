package com.roomledger.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreatePayVaRequest(
        @NotBlank String bookingRef,
        @Positive  long amount,               // in IDR (minor unit)
        @NotBlank String channelCode,         // e.g., "BNI_VIRTUAL_ACCOUNT"
        @NotBlank String displayName,
        Long expectedAmount
) {}
