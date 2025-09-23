package com.roomledger.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateQrisPaymentRequest(
        @NotNull @Positive Long amount
) {}