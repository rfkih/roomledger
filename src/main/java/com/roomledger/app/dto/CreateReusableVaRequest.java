package com.roomledger.app.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateReusableVaRequest(
        @NotBlank String customerId,
        @NotBlank String displayName,
        String channelCode // e.g., "BNI_VIRTUAL_ACCOUNT" | "BCA_VIRTUAL_ACCOUNT";
) {}
