package com.roomledger.app.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID tenantId,
        @NotNull UUID roomId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull BigDecimal depositAmount,
        @NotNull BillingMode billingMode                    // SINGLE_INVOICE or MONTHLY
) {
    public enum BillingMode { SINGLE_INVOICE, MONTHLY }
}

