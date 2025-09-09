package com.roomledger.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BillingQuoteResponse(
        UUID roomId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyPrice,
        int fullMonthsCount,
        BigDecimal discountRate,         // e.g. 0.05 for 5%
        BigDecimal subtotalBeforeDiscount,
        BigDecimal discountAmount,
        BigDecimal totalAfterDiscount,
        List<Line> lines                 // monthly breakdown
) {
    public record Line(
            String month,                  // "2025-08"
            int daysInMonth,
            int billableDays,
            BigDecimal dailyRate,
            BigDecimal lineSubtotal
    ) {}
}
