package com.roomledger.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequest(
        UUID tenantId,
        UUID roomId,
        LocalDate startDate,
        LocalDate endDate,         // optional
        BigDecimal depositAmount,
        BillingMode billingMode                    // SINGLE_INVOICE or MONTHLY
) {
    public enum BillingMode { SINGLE_INVOICE, MONTHLY }
}

