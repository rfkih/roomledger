package com.roomledger.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PendingBillDto(
        UUID paymentId,
        String type,                // DEPOSIT | RENT
        BigDecimal amount,
        LocalDate periodMonth,      // yyyy-MM-01 for RENT, null for DEPOSIT
        LocalDateTime createdAt
) {}
